package com.dusk.module.auth.registry.service;

import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.auth.registry.ResourceVersionCalculator;
import com.dusk.common.core.auth.registry.SnapshotResource;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.authz.ResourceRegistrationResult;
import com.dusk.module.auth.authz.service.AuthzEventPublisher;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.registry.config.RegistryProperties;
import com.dusk.module.auth.registry.diff.PermissionRoleLookup;
import com.dusk.module.auth.registry.diff.ResourceBinding;
import com.dusk.module.auth.registry.diff.ResourceDiffEntry;
import com.dusk.module.auth.registry.diff.ResourceKey;
import com.dusk.module.auth.registry.dto.RegistryRegistrationResponse;
import com.dusk.module.auth.registry.entity.*;
import com.dusk.module.auth.registry.enums.*;
import com.dusk.module.auth.registry.lifecycle.PermissionEvaluationInput;
import com.dusk.module.auth.registry.lifecycle.PermissionLifecycleEvaluator;
import com.dusk.module.auth.registry.lifecycle.PermissionTransition;
import com.dusk.module.auth.registry.repository.*;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 资源快照注册实现，见《权限优化方案-整理版》3.4 / 4.2 / 4.7。
 *
 * <p>本实现落实三条关键约束：</p>
 * <ol>
 *   <li><b>Release 不可变</b>：身份为 {@code (serviceId, serviceVersion, resourceVersion)}，
 *       快照内容变化即新 Release，迟到的旧请求不会改写已注册 Release；</li>
 *   <li><b>resourceRevision 单调</b>：任何影响有效资源并集的变更都推进服务级修订号，
 *       由 Outbox 事件与 Redis 投影按它排序；{@code resourceVersion} 只做幂等；</li>
 *   <li><b>换绑是发布门禁</b>：存在换绑时候选 Release 进入 {@link ReleaseStatus#PENDING_APPROVAL}，
 *       不参与运行映射，注册返回 {@code REBIND_APPROVAL_REQUIRED}。</li>
 * </ol>
 */
@Service
@Slf4j
public class ResourceRegistryServiceImpl implements IResourceRegistryService {

    /**
     * 参与运行映射的资源状态：换绑未确认期间候选 Release 不参与，因此这里不再包含
     * {@code PENDING_REBIND}/{@code REBIND_REJECTED}（4.7 已将换绑改为发布门禁）。
     */
    private static final List<ResourceStatus> EFFECTIVE_RESOURCE_STATUSES =
            List.of(ResourceStatus.ACTIVE);

    /**
     * 可能承接流量的 Release 状态。
     */
    private static final List<ReleaseStatus> TRAFFIC_STATUSES = ReleaseStatus.trafficCapableStatuses();

    private final IServiceDefinitionRepository serviceRepository;
    private final IServiceReleaseRepository releaseRepository;
    private final IApiResourceRepository resourceRepository;
    private final IPermissionDefinitionRepository permissionRepository;
    private final IGrantPermissionRepository grantPermissionRepository;
    private final IRebindRequestRepository rebindRequestRepository;
    private final RegistryProperties properties;
    private final ResourceReadModelProjector readModelProjector;
    private final AuthzEventPublisher authzEventPublisher;
    private final IAuthPermissionManager authPermissionManager;

    /**
     * 纯函数协作者：无状态、不依赖 Spring 上下文。
     */
    private final RegistryApplyPlanner planner = new RegistryApplyPlanner();
    private final PermissionLifecycleEvaluator permissionEvaluator = new PermissionLifecycleEvaluator();

    public ResourceRegistryServiceImpl(IServiceDefinitionRepository serviceRepository,
                                       IServiceReleaseRepository releaseRepository,
                                       IApiResourceRepository resourceRepository,
                                       IPermissionDefinitionRepository permissionRepository,
                                       IGrantPermissionRepository grantPermissionRepository,
                                       IRebindRequestRepository rebindRequestRepository,
                                       RegistryProperties properties,
                                       ResourceReadModelProjector readModelProjector,
                                       AuthzEventPublisher authzEventPublisher,
                                       IAuthPermissionManager authPermissionManager) {
        this.serviceRepository = serviceRepository;
        this.releaseRepository = releaseRepository;
        this.resourceRepository = resourceRepository;
        this.permissionRepository = permissionRepository;
        this.grantPermissionRepository = grantPermissionRepository;
        this.rebindRequestRepository = rebindRequestRepository;
        this.properties = properties;
        this.readModelProjector = readModelProjector;
        this.authzEventPublisher = authzEventPublisher;
        this.authPermissionManager = authPermissionManager;
    }

    private static void addPermission(Set<String> codes, Permission permission) {
        if (permission == null) {
            return;
        }
        if (permission.getName() != null) {
            codes.add(permission.getName());
        }
        if (permission.getChildren() != null) {
            permission.getChildren().forEach(child -> addPermission(codes, child));
        }
    }

    @Override
    @Transactional
    public RegistryRegistrationResponse register(ResourceSnapshot snapshot) {
        return execute(snapshot).response();
    }

    @Override
    @Transactional
    public ResourceRegistrationResult registerSnapshot(ResourceSnapshot snapshot) {
        try {
            return execute(snapshot).result();
        } catch (UnknownPermissionException e) {
            log.error("快照注册被拒绝：{}", e.getMessage());
            return ResourceRegistrationResult.unknownPermission(e.getMessage());
        } catch (BusinessException e) {
            log.error("快照注册被拒绝：{}", e.getMessage());
            return ResourceRegistrationResult.invalid(e.getMessage());
        }
    }

    /**
     * 事务内执行注册，同时产出「给 SDK 的稳定结果」与「给运维的详细报告」。
     */
    private RegistrationExecution execute(ResourceSnapshot snapshot) {
        validateResourceVersion(snapshot);
        validatePermissionCatalogue(snapshot);
        LocalDateTime now = LocalDateTime.now();
        String serviceId = snapshot.serviceId();

        ServiceDefinition service = resolveServiceForUpdate(serviceId);
        List<ServiceRelease> sameVersionTraffic = trafficReleasesOfVersion(serviceId, snapshot.serviceVersion(), null);

        Optional<ServiceRelease> identicalRelease = releaseRepository
                .findByService_ServiceIdAndServiceVersionAndResourceVersion(
                        serviceId, snapshot.serviceVersion(), snapshot.resourceVersion());
        if (identicalRelease.isPresent()) {
            return handleIdentical(service, identicalRelease.get(), snapshot);
        }

        ServiceRelease release = createRelease(service, snapshot);
        List<RegisteredResource> existingResources = List.of();
        List<ResourceBinding> baseBindings = predecessorBindings(service, release);
        RegistryApplyPlan plan = planner.plan(existingResources, baseBindings, snapshot, roleLookup());

        applyMutations(release, existingResources, plan, now);

        if (!plan.diff().rebinds().isEmpty()) {
            List<RebindRequest> pending = recordRebindRequests(service, release, plan, now);
            release.setStatus(ReleaseStatus.PENDING_APPROVAL);
            releaseRepository.save(release);
            // 候选 Release 不参与有效并集，因此不推进 revision；但仍重算权限状态，
            // 让旧绑定保持 ACTIVE、候选绑定处于 ORPHANED（如果它尚未被任何有效资源引用）。
            PermissionReconciliation reconciliation = reconcilePermissions(plan.touchedPermissions(), serviceId, now);

            log.warn("快照注册发现权限换绑，候选 Release 已挂起等待审批（新实例不得接流量）："
                            + "serviceId={}, serviceVersion={}, resourceVersion={}, 待办={}",
                    serviceId, snapshot.serviceVersion(), snapshot.resourceVersion(),
                    pending.stream().map(RebindRequest::getId).toList());
            return new RegistrationExecution(
                    response(snapshot, false, release, plan, reconciliation, pending),
                    ResourceRegistrationResult.rebindRequired(String.valueOf(release.getId()),
                            service.getResourceRevision(), String.valueOf(pending.getFirst().getId())));
        }

        Optional<String> conflict = findActiveConflict(serviceId, release, plan, sameVersionTraffic);
        if (conflict.isPresent()) {
            release.setStatus(ReleaseStatus.BLOCKED);
            releaseRepository.save(release);
            log.error("快照注册被阻塞：候选 Release 与运行中的 Release 存在同路径不同权限，"
                    + "必须摘除旧 Release 流量或使用版本化路径：serviceId={}, conflict={}", serviceId, conflict.get());
            return new RegistrationExecution(
                    response(snapshot, false, release, plan, new PermissionReconciliation(List.of(), List.of()), List.of()),
                    ResourceRegistrationResult.activeConflict(String.valueOf(release.getId()),
                            service.getResourceRevision()));
        }

        retireSameVersionReleases(sameVersionTraffic, release, now);
        PermissionReconciliation reconciliation = reconcilePermissions(plan.touchedPermissions(), serviceId, now);
        release.setStatus(ReleaseStatus.REGISTERED);
        releaseRepository.save(release);
        service.nextResourceRevision();
        serviceRepository.save(service);
        scheduleRegistrationSideEffects(serviceId, snapshot.resourceVersion(), snapshot.serviceVersion(),
                service.getResourceRevision());

        log.info("快照注册完成：serviceId={}, serviceVersion={}, resourceVersion={}, revision={}，"
                        + "新增 {}、更新 {}、废弃 {}，权限生效 {}、孤立 {}",
                serviceId, snapshot.serviceVersion(), snapshot.resourceVersion(), service.getResourceRevision(),
                plan.ofType(ResourceDiffType.ADD).size(), plan.ofType(ResourceDiffType.UPDATE).size(),
                plan.ofType(ResourceDiffType.REMOVE).size(),
                reconciliation.activated().size(), reconciliation.orphaned().size());

        return new RegistrationExecution(
                response(snapshot, false, release, plan, reconciliation, List.of()),
                ResourceRegistrationResult.accepted(String.valueOf(release.getId()), service.getResourceRevision()));
    }

    /**
     * 3.3 契约校验：快照声明的 {@code resourceVersion} 必须与其资源内容一致。
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
     * 相同 Release 身份：幂等成功，不重复推进 revision。
     */
    private RegistrationExecution handleIdentical(ServiceDefinition service, ServiceRelease release,
                                                  ResourceSnapshot snapshot) {
        if (release.getStatus() == ReleaseStatus.PENDING_APPROVAL) {
            List<RebindRequest> pending = rebindRequestRepository.findByCandidateReleaseIdAndStatusIn(
                    release.getId(), List.of(RebindStatus.PENDING));
            String pendingId = pending.isEmpty() ? null : String.valueOf(pending.getFirst().getId());
            return new RegistrationExecution(
                    RegistryRegistrationResponse.unchanged(snapshot.serviceId(), snapshot.serviceVersion(),
                            snapshot.resourceVersion(), release.getStatus().name(), snapshot.totalResources()),
                    ResourceRegistrationResult.rebindRequired(String.valueOf(release.getId()),
                            service.getResourceRevision(), pendingId));
        }
        if (release.getStatus() == ReleaseStatus.BLOCKED) {
            List<RebindRequest> rebinds = rebindRequestRepository.findByCandidateReleaseId(release.getId());
            String lastId = rebinds.isEmpty() ? null : String.valueOf(rebinds.getLast().getId());
            log.warn("候选 Release 仍处于 BLOCKED，拒绝就绪：serviceId={}, serviceVersion={}, resourceVersion={}",
                    snapshot.serviceId(), snapshot.serviceVersion(), snapshot.resourceVersion());
            return new RegistrationExecution(
                    RegistryRegistrationResponse.unchanged(snapshot.serviceId(), snapshot.serviceVersion(),
                            snapshot.resourceVersion(), release.getStatus().name(), snapshot.totalResources()),
                    new ResourceRegistrationResult(ResourceRegistrationResult.ACTIVE_RELEASE_CONFLICT,
                            String.valueOf(release.getId()), service.getResourceRevision(), lastId));
        }

        // 幂等命中仍然投影一次读模型：Pod 重启会重复上报同一个版本，这条路径可以自愈缓存。
        scheduleReadModelProjection(snapshot.serviceId());
        log.info("快照注册幂等命中，未做写库：serviceId={}, serviceVersion={}, resourceVersion={}, 资源数={}",
                snapshot.serviceId(), snapshot.serviceVersion(), snapshot.resourceVersion(), snapshot.totalResources());
        return new RegistrationExecution(
                RegistryRegistrationResponse.unchanged(snapshot.serviceId(), snapshot.serviceVersion(),
                        snapshot.resourceVersion(), release.getStatus().name(), snapshot.totalResources()),
                ResourceRegistrationResult.unchanged(String.valueOf(release.getId()),
                        service.getResourceRevision()));
    }

    /**
     * 3.1：SDK 只能引用权限目录中已存在的权限码。
     *
     * <p>权限目录为空时不阻断（本地开发/权限树尚未接入），但输出 WARN，避免把「未配置」
     * 伪装成「全部合法」。</p>
     */
    private void validatePermissionCatalogue(ResourceSnapshot snapshot) {
        if (!properties.isStrictPermissionCatalogue()) {
            return;
        }
        Set<String> known = knownPermissionCodes();
        if (known.isEmpty()) {
            log.warn("权限目录为空，跳过权限码校验（请确认各模块 *AuthProvider 已向 Auth 注册权限树）：serviceId={}",
                    snapshot.serviceId());
            return;
        }
        for (SnapshotResource resource : snapshot.resources()) {
            if (ResourceBinding.ANONYMOUS_PERMISSION.equals(resource.permission())) {
                continue;
            }
            if (!known.contains(resource.permission())) {
                throw new UnknownPermissionException(resource.permission());
            }
        }
    }

    private Set<String> knownPermissionCodes() {
        Set<String> codes = new HashSet<>();
        try {
            for (Permission permission : authPermissionManager.getDefinitionPermissionTree(false)) {
                addPermission(codes, permission);
            }
            for (Permission permission : authPermissionManager.getDefinitionPermissionTree(true)) {
                addPermission(codes, permission);
            }
        } catch (Exception e) {
            log.warn("读取权限目录失败，本次跳过权限码校验", e);
            return Set.of();
        }
        return codes;
    }

    /**
     * 解析或创建 Service，并锁定服务行以串行化 revision 推进（3.2）。
     */
    private ServiceDefinition resolveServiceForUpdate(String serviceId) {
        Optional<ServiceDefinition> existing = serviceRepository.findByServiceIdForUpdate(serviceId);
        if (existing.isPresent()) {
            return existing.get();
        }
        ServiceDefinition created = new ServiceDefinition();
        created.setServiceId(serviceId);
        log.info("权限注册：首次登记服务 serviceId={}", serviceId);
        // 并发首次注册由 index_registry_service_service_id 唯一索引兜底；冲突让本次请求失败，
        // SDK 的幂等重试会读到另一个实例刚提交的记录。
        return serviceRepository.saveAndFlush(created);
    }

    /**
     * 按不可变身份创建 Release（4.2）。内容变化即新 Release，绝不改写旧 Release。
     */
    private ServiceRelease createRelease(ServiceDefinition service, ResourceSnapshot snapshot) {
        ServiceRelease created = new ServiceRelease();
        created.setService(service);
        created.setServiceVersion(snapshot.serviceVersion());
        created.setResourceVersion(snapshot.resourceVersion());
        created.setStatus(ReleaseStatus.REGISTERED);
        log.info("权限注册：登记新的发布身份 serviceId={}, serviceVersion={}, resourceVersion={}",
                service.getServiceId(), snapshot.serviceVersion(), snapshot.resourceVersion());
        return releaseRepository.saveAndFlush(created);
    }

    /**
     * 同一 serviceVersion 下、可能承接流量的其他 Release。
     */
    private List<ServiceRelease> trafficReleasesOfVersion(String serviceId, String serviceVersion, Long excludeId) {
        return releaseRepository.findByService_ServiceId(serviceId).stream()
                .filter(release -> java.util.Objects.equals(release.getServiceVersion(), serviceVersion))
                .filter(release -> excludeId == null || !excludeId.equals(release.getId()))
                .filter(release -> release.getStatus() != null && release.getStatus().mayServeTraffic())
                .toList();
    }

    /**
     * Diff 基准 = 最近登记的、可能承接流量的 Release。
     *
     * <p>候选 Release 不参与基准，否则等待审批的候选会污染后续注册。</p>
     */
    private List<ResourceBinding> predecessorBindings(ServiceDefinition service, ServiceRelease current) {
        List<ServiceRelease> releases = releaseRepository.findByService_ServiceId(service.getServiceId());
        Comparator<ServiceRelease> byRecency = Comparator
                .comparing(ServiceRelease::getCreateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ServiceRelease::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        return releases.stream()
                .filter(release -> !release.getId().equals(current.getId()))
                .filter(release -> release.getStatus() != null && release.getStatus().mayServeTraffic())
                .max(byRecency)
                .map(predecessor -> toRegisteredResources(
                        resourceRepository.findByRelease_Id(predecessor.getId()), service.getServiceId())
                        .stream().map(RegisteredResource::binding).toList())
                .orElse(List.of());
    }

    /**
     * 与运行中的 Release 比较同一 {@code (method, path)} 的绑定。
     *
     * <p>同路径不同权限不能同时进入有效并集（4.7）：这正是 REBIND 审批后仍需要等待旧 Release
     * 摘流的原因。返回的字符串会写入日志和 {@code activationBlockedReason}。</p>
     */
    private Optional<String> findActiveConflict(String serviceId, ServiceRelease candidate,
                                                RegistryApplyPlan plan,
                                                List<ServiceRelease> sameVersionTraffic) {
        List<Long> releaseIds = new ArrayList<>();
        for (ServiceRelease release : releaseRepository.findByService_ServiceId(serviceId)) {
            if (release.getId().equals(candidate.getId())) {
                continue;
            }
            if (release.getStatus() != null && release.getStatus().mayServeTraffic()) {
                releaseIds.add(release.getId());
            }
        }
        if (releaseIds.isEmpty()) {
            return Optional.empty();
        }
        Map<ResourceKey, String> activeBindings = new HashMap<>();
        for (ApiResource resource : resourceRepository.findByRelease_IdInAndStatusIn(releaseIds,
                EFFECTIVE_RESOURCE_STATUSES)) {
            if (resource.getPermissionCode() == null || resource.getPermissionCode().isBlank()) {
                continue;
            }
            activeBindings.put(ResourceKey.of(resource.getHttpMethod(), resource.getPath()), resource.getPermissionCode());
        }
        for (ResourceMutation mutation : plan.mutations()) {
            String active = activeBindings.get(mutation.key());
            if (active != null && !active.equals(mutation.permissionCode())) {
                return Optional.of(mutation.key().asText() + " 运行中=" + active + "，候选=" + mutation.permissionCode());
            }
        }
        return Optional.empty();
    }

    /**
     * 同一 serviceVersion 被新的 resourceVersion 取代时，旧 Release 必须退出有效并集。
     *
     * <p>同一个构建版本无法通过 Nacos metadata 区分两份不同的资源定义，保留旧 Release 会
     * 形成「旧快照复活」；因此这里直接退役旧身份。跨 serviceVersion 的滚动发布不受影响。</p>
     */
    private void retireSameVersionReleases(List<ServiceRelease> releases, ServiceRelease current, LocalDateTime now) {
        for (ServiceRelease old : releases) {
            if (old.getId().equals(current.getId())) {
                continue;
            }
            if (old.getStatus() != null && old.getStatus().mayServeTraffic()) {
                old.setStatus(ReleaseStatus.RETIRED);
                old.setRetiredAt(now);
                releaseRepository.save(old);
                log.info("同一发布版本被新快照取代，旧 Release 退役：releaseId={}, serviceId={}, serviceVersion={}, resourceVersion={}",
                        old.getId(), old.getService().getServiceId(), old.getServiceVersion(), old.getResourceVersion());
            }
        }
    }

    /**
     * 创建/复用换绑待办。同一候选 Release 的同一路径 + 新权限只建一条（4.7 第 5 点）。
     */
    private List<RebindRequest> recordRebindRequests(ServiceDefinition service, ServiceRelease release,
                                                     RegistryApplyPlan plan, LocalDateTime now) {
        List<RebindRequest> requests = new ArrayList<>();
        for (ResourceDiffEntry entry : plan.diff().rebinds()) {
            RebindRequest request = rebindRequestRepository
                    .findByCandidateReleaseIdAndHttpMethodAndPathAndNewPermissionCode(
                            release.getId(), entry.key().method(), entry.key().path(),
                            entry.currentPermission() == null ? "" : entry.currentPermission())
                    .orElseGet(() -> {
                        RebindRequest created = new RebindRequest();
                        created.setServiceId(service.getServiceId());
                        created.setCandidateReleaseId(release.getId());
                        created.setHttpMethod(entry.key().method());
                        created.setPath(entry.key().path());
                        created.setOldPermissionCode(entry.previousPermission());
                        created.setNewPermissionCode(entry.currentPermission());
                        created.setStatus(RebindStatus.PENDING);
                        created.setRequestedAt(now);
                        return created;
                    });
            request.setActivationBlockedReason("等待管理员审批");
            requests.add(rebindRequestRepository.saveAndFlush(request));
        }
        return requests;
    }

    /**
     * 施加资源变更（按快照物化 + 换绑保护）。
     */
    private void applyMutations(ServiceRelease release,
                                List<RegisteredResource> existingResources,
                                RegistryApplyPlan plan,
                                LocalDateTime now) {
        if (plan.mutations().isEmpty()) {
            return;
        }
        Map<ResourceKey, ApiResource> rowByKey = new HashMap<>();
        for (ApiResource row : resourceRepository.findByRelease_Id(release.getId())) {
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
     * 权限创建与状态重算（4.8）。{@code DISABLED} 只进不出自动链路。
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
        log.info("权限注册：登记新的权限点 code={}, sourceServiceId={}", code, serviceId);
        return permissionRepository.save(created);
    }

    private boolean isReferencedByEffectiveResource(String permissionCode) {
        List<ApiResource> references = resourceRepository.findPermissionReferences(permissionCode, TRAFFIC_STATUSES);
        return references.stream()
                .anyMatch(reference -> reference.getStatus() != null && reference.getStatus().effectiveForRuntime());
    }

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

    private void scheduleReadModelProjection(String serviceId) {
        if (!properties.getReadModel().isEnabled()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
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

    private void scheduleRegistrationSideEffects(String serviceId, String resourceVersion, String serviceVersion,
                                                 long resourceRevision) {
        scheduleReadModelProjection(serviceId);
        authzEventPublisher.resourceChanged(serviceId, resourceVersion, serviceVersion, resourceRevision);
    }

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

    private RegistryRegistrationResponse response(ResourceSnapshot snapshot, boolean unchanged, ServiceRelease release,
                                                  RegistryApplyPlan plan, PermissionReconciliation reconciliation,
                                                  Collection<RebindRequest> pending) {
        return new RegistryRegistrationResponse(
                snapshot.serviceId(),
                snapshot.serviceVersion(),
                snapshot.resourceVersion(),
                unchanged,
                release.getStatus() == null ? null : release.getStatus().name(),
                snapshot.totalResources(),
                texts(plan, ResourceDiffType.ADD),
                texts(plan, ResourceDiffType.UPDATE),
                texts(plan, ResourceDiffType.REMOVE),
                texts(plan, ResourceDiffType.REBIND),
                reconciliation.activated(),
                reconciliation.orphaned());
    }

    private static List<String> texts(RegistryApplyPlan plan, ResourceDiffType type) {
        return plan.ofType(type).stream().map(ResourceMutation::asText).toList();
    }

    /**
     * 本次注册的两种视图：SDK 只看 {@link ResourceRegistrationResult}，运维看详细报告。
     */
    private record RegistrationExecution(RegistryRegistrationResponse response, ResourceRegistrationResult result) {
    }

    /**
     * 本次注册后权限码的分布，用于响应体与日志。
     */
    private record PermissionReconciliation(List<String> activated, List<String> orphaned) {
    }
}
