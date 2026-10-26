package com.dusk.module.auth.registry.service;

import com.dusk.common.core.auth.registry.ResourceVersionCalculator;
import com.dusk.common.rpc.authz.ResourceRuntimeSnapshot;
import com.dusk.module.auth.registry.diff.ResourceKey;
import com.dusk.module.auth.registry.entity.ApiResource;
import com.dusk.module.auth.registry.entity.ServiceDefinition;
import com.dusk.module.auth.registry.entity.ServiceRelease;
import com.dusk.module.auth.registry.enums.ResourceStatus;
import com.dusk.module.auth.registry.repository.IApiResourceRepository;
import com.dusk.module.auth.registry.repository.IServiceDefinitionRepository;
import com.dusk.module.auth.registry.repository.IServiceReleaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 有效资源并集解析，见《权限优化方案-整理版》4.5 / 5.2。
 *
 * <p>Redis 投影与 Dubbo 回源共用本类，保证「L2 与回源」看到的是同一份规则，
 * 不会出现投影按一套仲裁、回源按另一套仲裁的漂移。</p>
 *
 * <p>只从 PostgreSQL 读取，且内容与 {@code resourceRevision} 来自同一次事务读取
 * （{@link Transactional#readOnly()} + 单次查询序列）。冲突以默认 Release 为准并告警。</p>
 */
@Service
@Slf4j
public class ResourceRuntimeResolver {

    /**
     * 参与运行映射的资源状态：换绑候选 Release 本身不参与（4.7 发布门禁）。
     */
    private static final List<ResourceStatus> EFFECTIVE_RESOURCE_STATUSES = List.of(ResourceStatus.ACTIVE);

    private final IServiceDefinitionRepository serviceRepository;
    private final IServiceReleaseRepository releaseRepository;
    private final IApiResourceRepository resourceRepository;

    public ResourceRuntimeResolver(IServiceDefinitionRepository serviceRepository,
                                   IServiceReleaseRepository releaseRepository,
                                   IApiResourceRepository resourceRepository) {
        this.serviceRepository = serviceRepository;
        this.releaseRepository = releaseRepository;
        this.resourceRepository = resourceRepository;
    }

    /**
     * 解析某服务的有效资源并集。
     *
     * @return 服务未登记或没有可承接流量的 Release 时返回 {@link Optional#empty()}
     */
    @Transactional(readOnly = true)
    public Optional<ResourceRuntimeSnapshot> resolve(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            return Optional.empty();
        }
        Optional<ServiceDefinition> service = serviceRepository.findByServiceId(serviceId);
        if (service.isEmpty()) {
            log.warn("运行读模型：Auth 侧没有该服务的注册记录，serviceId={}", serviceId);
            return Optional.empty();
        }

        List<ServiceRelease> candidates = new ArrayList<>();
        for (ServiceRelease release : releaseRepository.findByService_ServiceId(serviceId)) {
            if (release.getStatus() != null && release.getStatus().mayServeTraffic()) {
                candidates.add(release);
            }
        }
        if (candidates.isEmpty()) {
            log.warn("运行读模型：该服务没有可能承接流量的 Release（全部已退役或被阻塞），serviceId={}", serviceId);
            return Optional.empty();
        }

        ServiceRelease defaultRelease = resolveDefaultRelease(service.get(), candidates);
        candidates.sort(Comparator
                .comparing((ServiceRelease release) -> !release.getId().equals(defaultRelease.getId()))
                .thenComparing(ServiceRelease::getCreateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ServiceRelease::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Long> releaseIds = candidates.stream().map(ServiceRelease::getId).toList();
        List<ApiResource> resources = resourceRepository
                .findByRelease_IdInAndStatusIn(releaseIds, EFFECTIVE_RESOURCE_STATUSES);

        Map<ResourceKey, String> byKey = new LinkedHashMap<>();
        resources.stream()
                .sorted(Comparator.comparing((ApiResource resource) -> releaseIds.indexOf(resource.getRelease().getId()))
                        .thenComparing(ApiResource::getHttpMethod)
                        .thenComparing(ApiResource::getPath))
                .forEach(resource -> merge(byKey, resource, defaultRelease, serviceId));

        List<ResourceRuntimeSnapshot.RuntimeRoute> protectedRoutes = new ArrayList<>();
        List<ResourceRuntimeSnapshot.RuntimeAnonymousResource> anonymousRoutes = new ArrayList<>();
        for (Map.Entry<ResourceKey, String> entry : byKey.entrySet()) {
            ResourceKey key = entry.getKey();
            String permission = entry.getValue();
            if (ResourceVersionCalculator.ANONYMOUS_PERMISSION.equals(permission)) {
                anonymousRoutes.add(new ResourceRuntimeSnapshot.RuntimeAnonymousResource(key.method(), key.path()));
            } else {
                protectedRoutes.add(new ResourceRuntimeSnapshot.RuntimeRoute(key.method(), key.path(), permission));
            }
        }

        return Optional.of(new ResourceRuntimeSnapshot(
                serviceId,
                defaultRelease.getResourceVersion() == null ? "" : defaultRelease.getResourceVersion(),
                defaultRelease.getServiceVersion(),
                service.get().getResourceRevision(),
                protectedRoutes,
                anonymousRoutes));
    }

    private void merge(Map<ResourceKey, String> target, ApiResource resource,
                       ServiceRelease defaultRelease, String serviceId) {
        String permission = resource.getPermissionCode();
        if (permission == null || permission.isBlank()) {
            log.error("运行读模型跳过一条权限码为空的资源（数据异常，需人工核对）：resourceId={}, {} {}",
                    resource.getId(), resource.getHttpMethod(), resource.getPath());
            return;
        }
        ResourceKey key = ResourceKey.of(resource.getHttpMethod(), resource.getPath());
        String existing = target.putIfAbsent(key, permission);
        if (existing != null && !existing.equals(permission)) {
            log.warn("运行读模型存在绑定冲突，已按默认 Release 取定：serviceId={}, route={}, 采用={}（来自 Release {}），忽略={}（来自 Release {}）",
                    serviceId, key.asText(), existing, defaultRelease.getServiceVersion(),
                    permission, resource.getRelease().getServiceVersion());
        }
    }

    /**
     * 默认 Release：优先取部署系统标记；没有标记时取最近登记的 Release。
     */
    private ServiceRelease resolveDefaultRelease(ServiceDefinition service, List<ServiceRelease> candidates) {
        String marked = service.getDefaultReleaseVersion();
        if (marked != null && !marked.isBlank()) {
            Optional<ServiceRelease> matched = candidates.stream()
                    .filter(release -> marked.equals(release.getServiceVersion()))
                    .findFirst();
            if (matched.isPresent()) {
                return matched.get();
            }
            log.warn("运行读模型：部署系统标记的默认版本 {} 不在可能承接流量的 Release 中，回退为最近登记的 Release，serviceId={}",
                    marked, service.getServiceId());
        }
        return candidates.stream()
                .max(Comparator
                        .comparing(ServiceRelease::getCreateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(ServiceRelease::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow();
    }
}
