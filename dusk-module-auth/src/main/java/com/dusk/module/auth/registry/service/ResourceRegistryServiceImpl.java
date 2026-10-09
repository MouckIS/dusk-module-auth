package com.dusk.module.auth.registry.service;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.auth.registry.ResourceVersionCalculator;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.config.RegistryProperties;
import com.dusk.module.auth.registry.diff.PermissionRoleLookup;
import com.dusk.module.auth.registry.diff.ResourceBinding;
import com.dusk.module.auth.registry.diff.ResourceKey;
import com.dusk.module.auth.registry.dto.RegistryRegistrationResponse;
import com.dusk.module.auth.registry.entity.ApiResource;
import com.dusk.module.auth.registry.entity.PermissionDefinition;
import com.dusk.module.auth.registry.entity.ServiceDefinition;
import com.dusk.module.auth.registry.entity.ServiceRelease;
import com.dusk.module.auth.registry.enums.PermissionStatus;
import com.dusk.module.auth.registry.enums.ReleaseStatus;
import com.dusk.module.auth.registry.enums.ResourceDiffType;
import com.dusk.module.auth.registry.enums.ResourceStatus;
import com.dusk.module.auth.registry.lifecycle.PermissionEvaluationInput;
import com.dusk.module.auth.registry.lifecycle.PermissionLifecycleEvaluator;
import com.dusk.module.auth.registry.lifecycle.PermissionTransition;
import com.dusk.module.auth.registry.repository.IApiResourceRepository;
import com.dusk.module.auth.registry.repository.IPermissionDefinitionRepository;
import com.dusk.module.auth.registry.repository.IServiceDefinitionRepository;
import com.dusk.module.auth.registry.repository.IServiceReleaseRepository;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 资源快照注册实现，见《权限优化方案-整理版》3.4「Auth 事务内处理明细」。
 *
 * <p>事务内的顺序与文档一致：解析 Service/Release → 比较 resourceVersion → Diff → 资源落库 →
 * 权限创建与状态更新 → 提交；Redis 投影放在提交之后（3.8：Redis 是投影，不能先于 PG 事实落地）。</p>
 *
 * <p><b>关键取舍（都写在对应方法上）</b>：</p>
 * <ul>
 *   <li>幂等命中（版本未变）时仍然刷新读模型——Pod 重启会走这条路径，正好能自愈缓存；</li>
 *   <li>「新 Release 的代表集」是快照本身而非 Diff 结果，见 {@link RegistryApplyPlanner}；</li>
 *   <li>权限生效性判定同时计入换绑候选（4.9），方向保守，不会回收仍在被声明的权限。</li>
 * </ul>
 */
@Service
@Slf4j
public class ResourceRegistryServiceImpl implements IResourceRegistryService {

    private final IServiceDefinitionRepository serviceRepository;
    private final IServiceReleaseRepository releaseRepository;
    private final IApiResourceRepository resourceRepository;
    private final IPermissionDefinitionRepository permissionRepository;
    private final IGrantPermissionRepository grantPermissionRepository;
    private final RegistryProperties properties;
    private final ResourceReadModelProjector readModelProjector;

    /**
     * 纯函数协作者：无状态、不依赖 Spring 上下文，直接构造即可（与模块内 MapStruct Mapper 的处理方式一致）。
     */
    private final RegistryApplyPlanner planner = new RegistryApplyPlanner();
    private final PermissionLifecycleEvaluator permissionEvaluator = new PermissionLifecycleEvaluator();

    public ResourceRegistryServiceImpl(IServiceDefinitionRepository serviceRepository,
                                       IServiceReleaseRepository releaseRepository,
                                       IApiResourceRepository resourceRepository,
                                       IPermissionDefinitionRepository permissionRepository,
                                       IGrantPermissionRepository grantPermissionRepository,
                                       RegistryProperties properties,
                                       ResourceReadModelProjector readModelProjector) {
        this.serviceRepository = serviceRepository;
        this.releaseRepository = releaseRepository;
        this.resourceRepository = resourceRepository;
        this.permissionRepository = permissionRepository;
        this.grantPermissionRepository = grantPermissionRepository;
        this.properties = properties;
        this.readModelProjector = readModelProjector;
    }

    @Override
    @Transactional
    public RegistryRegistrationResponse register(ResourceSnapshot snapshot) {
        validateResourceVersion(snapshot);
        LocalDateTime now = LocalDateTime.now();
        String serviceId = snapshot.serviceId();

        ServiceDefinition service = resolveService(serviceId);
        ServiceRelease release = resolveRelease(service, snapshot.serviceVersion());
        List<ApiResource> existingRows = resourceRepository.findByRelease_Id(release.getId());

        if (snapshot.resourceVersion().equals(release.getResourceVersion())) {
            // 幂等命中：不写库，但仍然投影一次读模型——Pod 重启会重复上报同一个版本，
            // 这条路径正好可以用来修复「投影曾经失败 / L2 TTL 到期」导致的缓存缺失。
            scheduleReadModelProjection(serviceId);
            log.info("快照注册幂等命中，未做写库：serviceId={}, serviceVersion={}, resourceVersion={}, 资源数={}",
                    serviceId, snapshot.serviceVersion(), snapshot.resourceVersion(), snapshot.totalResources());
            return RegistryRegistrationResponse.unchanged(serviceId, snapshot.serviceVersion(),
                    snapshot.resourceVersion(), release.getStatus().name(), snapshot.totalResources());
        }

        List<RegisteredResource> existingResources = toRegisteredResources(existingRows, serviceId);
        List<ResourceBinding> baseBindings = release.getResourceVersion() == null
                ? predecessorBindings(service, release)
                : existingResources.stream().map(RegisteredResource::binding).toList();

        RegistryApplyPlan plan = planner.plan(existingResources, baseBindings, snapshot, roleLookup());

        applyMutations(release, existingRows, plan, now);
        PermissionReconciliation reconciliation = reconcilePermissions(plan.touchedPermissions(), serviceId, now);

        release.setResourceVersion(snapshot.resourceVersion());
        releaseRepository.save(release);

        scheduleReadModelProjection(serviceId);

        log.info("快照注册完成：serviceId={}, serviceVersion={}, resourceVersion={}，"
                        + "新增 {}、更新 {}、废弃 {}、换绑挂起 {}，权限生效 {}、孤立 {}",
                serviceId, snapshot.serviceVersion(), snapshot.resourceVersion(),
                plan.ofType(ResourceDiffType.ADD).size(), plan.ofType(ResourceDiffType.UPDATE).size(),
                plan.ofType(ResourceDiffType.REMOVE).size(), plan.ofType(ResourceDiffType.REBIND).size(),
                reconciliation.activated().size(), reconciliation.orphaned().size());
        if (!plan.diff().rebinds().isEmpty()) {
            // 4.9：换绑必须人工确认，这里只把待办「点名」，具体影响面已在 diff 中解析。
            log.warn("快照注册发现权限换绑，已挂起等待管理员确认（运行时仍按旧绑定执行）：serviceId={}, 换绑项={}",
                    serviceId, texts(plan, ResourceDiffType.REBIND));
        }

        return new RegistryRegistrationResponse(
                serviceId,
                snapshot.serviceVersion(),
                snapshot.resourceVersion(),
                false,
                release.getStatus().name(),
                snapshot.totalResources(),
                texts(plan, ResourceDiffType.ADD),
                texts(plan, ResourceDiffType.UPDATE),
                texts(plan, ResourceDiffType.REMOVE),
                texts(plan, ResourceDiffType.REBIND),
                reconciliation.activated(),
                reconciliation.orphaned());
    }

    /**
     * 3.3 契约校验：快照声明的 {@code resourceVersion} 必须与其资源内容一致。
     *
     * <p>该版本号是 Diff 幂等的唯一依据，一旦「内容变了而版本没变」，本次注册会被整体跳过，
     * 表现为新接口静默没有登记（5.10 下即 403）。拒绝注册比静默失配安全。</p>
     */
    private void validateResourceVersion(ResourceSnapshot snapshot) {
        if (!properties.isStrictResourceVersion() || snapshot.isResourceVersionConsistent()) {
            return;
        }
        throw new BusinessException("Resource Snapshot 校验失败：resourceVersion 与资源内容不一致（上报 "
                + snapshot.resourceVersion() + "，按 3.3 算法重算为 "
                + ResourceVersionCalculator.calculate(snapshot)
                + "）。请确认 SDK 与 Auth 使用同一版本的 Snapshot 契约。");
    }

    /**
     * 解析或创建 Service（2.2：Service 只承载标识信息，不承载权限事实）。
     */
    private ServiceDefinition resolveService(String serviceId) {
        Optional<ServiceDefinition> existing = serviceRepository.findByServiceId(serviceId);
        if (existing.isPresent()) {
            return existing.get();
        }
        ServiceDefinition created = new ServiceDefinition();
        created.setServiceId(serviceId);
        log.info("权限注册：首次登记服务 serviceId={}", serviceId);
        // 并发首次注册由 index_registry_service_service_id 唯一索引兜底：冲突会让本次请求 5xx，
        // SDK 对 5xx 会重试，届时即可读到另一个实例刚提交的记录（SyncRegisterClient 的既有语义）。
        return serviceRepository.save(created);
    }

    /**
     * 解析或创建 Release（4.5：Release 必须独立存在，否则无法表达滚动发布的多版本并存）。
     *
     * <p>新 Release 以 {@link ReleaseStatus#REGISTERED} 落地，由 4.7 的实例同步推进到 ACTIVE；
     * 在实例同步落地前，运行映射通过 {@link ReleaseStatus#mayServeTraffic()} 把 REGISTERED 一并纳入。</p>
     */
    private ServiceRelease resolveRelease(ServiceDefinition service, String serviceVersion) {
        Optional<ServiceRelease> existing =
                releaseRepository.findByService_ServiceIdAndServiceVersion(service.getServiceId(), serviceVersion);
        if (existing.isPresent()) {
            return existing.get();
        }
        ServiceRelease created = new ServiceRelease();
        created.setService(service);
        created.setServiceVersion(serviceVersion);
        created.setStatus(ReleaseStatus.REGISTERED);
        log.info("权限注册：登记新的发布版本 serviceId={}, serviceVersion={}", service.getServiceId(), serviceVersion);
        return releaseRepository.save(created);
    }

    /**
     * 首次注册某个 Release 时的 Diff 基准 = 上一个 Release 的资源（4.1/4.5）。
     *
     * <p>该基准只用于「产出 REMOVE 候选与 REBIND 识别」：旧 Release 的独有资源在其退役前必须继续生效（4.5），
     * 因此这些 REMOVE <b>不会</b>被施加到旧 Release 上（见 {@link RegistryApplyPlanner#plan}）。</p>
     */
    private List<ResourceBinding> predecessorBindings(ServiceDefinition service, ServiceRelease current) {
        List<ServiceRelease> releases = releaseRepository.findByService_ServiceId(service.getServiceId());
        Comparator<ServiceRelease> byRecency = Comparator
                .comparing(ServiceRelease::getCreateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ServiceRelease::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        return releases.stream()
                .filter(release -> !release.getId().equals(current.getId()))
                .max(byRecency)
                .map(predecessor -> toRegisteredResources(
                        resourceRepository.findByRelease_Id(predecessor.getId()), service.getServiceId())
                        .stream().map(RegisteredResource::binding).toList())
                .orElse(List.of());
    }

    /**
     * 施加资源变更（3.4 第 6~8 步）。
     */
    private void applyMutations(ServiceRelease release,
                                List<ApiResource> existingRows,
                                RegistryApplyPlan plan,
                                LocalDateTime now) {
        if (plan.mutations().isEmpty()) {
            return;
        }
        Map<ResourceKey, ApiResource> rowByKey = new HashMap<>();
        for (ApiResource row : existingRows) {
            if (row.getHttpMethod() == null || row.getPath() == null) {
                log.error("资源落库跳过结构异常的历史数据：resourceId={}", row.getId());
                continue;
            }
            rowByKey.put(ResourceKey.of(row.getHttpMethod(), row.getPath()), row);
        }

        List<ApiResource> toSave = new ArrayList<>(plan.mutations().size());
        for (ResourceMutation mutation : plan.mutations()) {
            ApiResource row = rowByKey.get(mutation.key());
            if (row == null) {
                row = new ApiResource();
                row.setRelease(release);
                row.setHttpMethod(mutation.method());
                row.setPath(mutation.path());
                row.setStatus(mutation.targetStatus());
                row.setStatusChangedAt(now);
            } else {
                ResourceStatus current = row.getStatus() == null ? ResourceStatus.ACTIVE : row.getStatus();
                if (current != mutation.targetStatus()) {
                    // 迁移合法性由枚举自己把关（非法迁移直接抛错），避免状态被静默改坏。
                    row.setStatus(current.transitionTo(mutation.targetStatus()));
                    row.setStatusChangedAt(now);
                }
            }
            if (mutation.resourceId() != null) {
                row.setResourceId(mutation.resourceId());
            }
            row.setPermissionCode(mutation.permissionCode());
            row.setPendingPermissionCode(mutation.pendingPermissionCode());
            toSave.add(row);
        }
        resourceRepository.saveAll(toSave);
    }

    /**
     * 权限创建与状态重算（3.4 第 9~10 步 / 4.8）。
     *
     * <p>对本次注册触及的每个权限码：先确保存在（缺失则新建为 ACTIVE），再用
     * {@link PermissionLifecycleEvaluator} 按「是否仍被可能承接流量的 Release 的有效资源引用」
     * 推演目标状态。{@code DISABLED} 只进不出自动链路，管理员的人工决策不会被部署动作回滚（4.3）。</p>
     *
     * <p><b>关于首次创建</b>：{@code DISABLED/ORPHANED} 的权限若被重新引用，会由评估器按 4.11 自动回到 ACTIVE，
     * 不需要重建权限，授权关系（RolePermission）也始终保留。</p>
     */
    private PermissionReconciliation reconcilePermissions(Set<String> permissionCodes,
                                                         String serviceId,
                                                         LocalDateTime now) {
        List<String> activated = new ArrayList<>();
        List<String> orphaned = new ArrayList<>();

        for (String code : permissionCodes) {
            PermissionDefinition permission = permissionRepository.findByCode(code)
                    .orElseGet(() -> createPermission(code, serviceId));

            PermissionTransition transition = permissionEvaluator.evaluate(new PermissionEvaluationInput(
                    code, permission.getStatus(), isReferencedByEffectiveResource(code)));
            if (transition.changed()) {
                permission.setStatus(transition.to());
                if (transition.to() == PermissionStatus.ORPHANED) {
                    permission.setOrphanedAt(now);
                }
                permissionRepository.save(permission);
                log.info("权限状态变更：{}", transition);
            }

            PermissionStatus finalStatus = permission.getStatus();
            if (finalStatus == PermissionStatus.ACTIVE) {
                activated.add(code);
            } else if (finalStatus == PermissionStatus.ORPHANED) {
                orphaned.add(code);
            }
        }
        return new PermissionReconciliation(activated, orphaned);
    }

    private PermissionDefinition createPermission(String code, String serviceId) {
        PermissionDefinition created = new PermissionDefinition();
        created.setCode(code);
        created.setSourceServiceId(serviceId);
        created.setStatus(PermissionStatus.ACTIVE);
        // displayName / multiTenancySides 留给权限树对账补齐（5.10 第 2 步的「权限码与权限树对账」）：
        // 注册链路只保证「API 声明了哪些权限码」，不臆造展示名与租户侧。
        log.info("权限注册：登记新的权限点 code={}, sourceServiceId={}", code, serviceId);
        return permissionRepository.save(created);
    }

    /**
     * 权限是否仍被「可能承接流量的 Release」下的有效资源引用（4.8）。
     *
     * <p>引用来源包含生效绑定与换绑候选（见 {@code findPermissionReferences} 的说明）。</p>
     */
    private boolean isReferencedByEffectiveResource(String permissionCode) {
        List<ApiResource> references = resourceRepository.findPermissionReferences(
                permissionCode, ReleaseStatus.trafficCapableStatuses());
        return references.stream()
                .anyMatch(reference -> reference.getStatus() != null && reference.getStatus().effectiveForRuntime());
    }

    /**
     * 换绑影响面查询（4.9）。失败只降级为「影响面未知」，不能让一次辅助查询拖垮注册。
     */
    private PermissionRoleLookup roleLookup() {
        return permissionCode -> {
            try {
                List<Long> roleIds = grantPermissionRepository.findRoleIdsByPermissionName(permissionCode);
                return roleIds == null ? Set.of() : new HashSet<>(roleIds);
            } catch (Exception e) {
                log.warn("换绑影响面查询失败，本次按「影响面未知」记录（不影响换绑挂起与注册结果）：permissionCode={}",
                        permissionCode, e);
                return Set.of();
            }
        };
    }

    /**
     * 事务提交后再投影读模型（3.4 第 12 步）。
     *
     * <p>事务内投影会把可能回滚的中间状态写进 Redis；而注册本身不能因为缓存写失败而失败——
     * PG 才是事实来源（红线⑧）。</p>
     */
    private void scheduleReadModelProjection(String serviceId) {
        if (!properties.getReadModel().isEnabled()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 正常不会发生（入口方法是 @Transactional）；兜底为同步投影，宁可投影早一点也不要落后。
            readModelProjector.project(serviceId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                readModelProjector.project(serviceId);
            }
        });
    }

    /**
     * 历史数据 → 落库计划的输入视图（绑定 + 状态）。
     *
     * <p>权限码为空的资源会被剔除并报错：把它当成匿名（{@code ANONYMOUS}）会让一条受保护路由静默变成
     * 「无需登录」，方向错误；剔除只是让这条异常数据不参与本次 Diff，需要人工核对。</p>
     */
    private List<RegisteredResource> toRegisteredResources(List<ApiResource> rows, String serviceId) {
        List<RegisteredResource> resources = new ArrayList<>(rows.size());
        for (ApiResource row : rows) {
            if (row.getHttpMethod() == null || row.getPath() == null) {
                log.error("Diff 基准跳过结构异常的历史数据：serviceId={}, resourceId={}", serviceId, row.getId());
                continue;
            }
            String permissionCode = row.getPermissionCode();
            if (permissionCode == null || permissionCode.isBlank()) {
                log.error("Diff 基准跳过权限码为空的资源（数据异常，需人工核对）：serviceId={}, resourceId={}, {} {}",
                        serviceId, row.getId(), row.getHttpMethod(), row.getPath());
                continue;
            }
            ResourceStatus status = row.getStatus() == null ? ResourceStatus.ACTIVE : row.getStatus();
            resources.add(RegisteredResource.of(
                    new ResourceBinding(row.getHttpMethod(), row.getPath(), permissionCode, row.getResourceId()),
                    status));
        }
        return resources;
    }

    private static List<String> texts(RegistryApplyPlan plan, ResourceDiffType type) {
        return plan.ofType(type).stream().map(ResourceMutation::asText).toList();
    }

    /**
     * 本次注册后权限码的分布，用于响应体与日志。
     */
    private record PermissionReconciliation(List<String> activated, List<String> orphaned) {
    }
}
