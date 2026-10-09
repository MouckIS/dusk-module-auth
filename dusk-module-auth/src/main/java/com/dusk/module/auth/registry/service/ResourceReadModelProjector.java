package com.dusk.module.auth.registry.service;

import com.dusk.common.core.auth.registry.ResourceVersionCalculator;
import com.dusk.module.auth.registry.config.RegistryProperties;
import com.dusk.module.auth.registry.diff.ResourceKey;
import com.dusk.module.auth.registry.entity.ApiResource;
import com.dusk.module.auth.registry.entity.ServiceDefinition;
import com.dusk.module.auth.registry.entity.ServiceRelease;
import com.dusk.module.auth.registry.enums.ReleaseStatus;
import com.dusk.module.auth.registry.enums.ResourceStatus;
import com.dusk.module.auth.registry.repository.IApiResourceRepository;
import com.dusk.module.auth.registry.repository.IServiceDefinitionRepository;
import com.dusk.module.auth.registry.repository.IServiceReleaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis 运行态读模型投影：{@code auth:resource:{serviceId}}，见《权限优化方案-整理版》3.4 第 12 步 / 5.7。
 *
 * <p>本类<b>只从 PostgreSQL 重建</b>，不做增量拼接：红线⑧ 规定 PG 是事实来源、
 * Redis 是可重建的投影，从 PG 全量重算比在缓存上打补丁更能保证两者一致。</p>
 *
 * <h3>与网关的契约</h3>
 * <p>key 前缀与 JSON 结构必须与网关侧的 {@code AuthorizationReadModelKeys.RESOURCE_PREFIX} /
 * {@code AuthorizationReadModelJson.parseServiceRouteModel} 逐字段对齐：
 * {@code {resourceVersion, releaseVersion, resources:[{method,path,permission}], anonymousResources:[{method,path}]}}。
 * 两侧跨仓库，这里用注释固定契约，改动前请同步网关。若结构不符，网关会把整份快照判为解析失败并按
 * 「未命中 → 回源」处理（解析器刻意不做静默容错），表现是鉴权全部退化为回源而不是错误放行。</p>
 *
 * <h3>单版本 + 并集 + 默认 Release 仲裁（5.7）</h3>
 * <p>注册模型是多 Release 的，但网关负载均衡时无法预知请求落到哪个实例，因此运行映射只能取
 * <b>所有可能承接流量的 Release 的并集</b>；同一 {@code (method, path)} 被不同 Release 绑定到
 * 不同权限时，以默认 Release 的绑定为准并告警（冲突需要从接口设计上避免，例如版本化路径）。</p>
 *
 * <h3>失败处理</h3>
 * <p>投影失败只打日志、不向上抛：注册事务已经提交，PG 已承载事实，
 * 让调用方因为缓存写失败而回滚一个已经提交的注册反而是错的。
 * 3.8 的 Outbox Publisher 落地后会在此基础上补重试与补偿。</p>
 */
@Component
@Slf4j
public class ResourceReadModelProjector {

    /**
     * 读模型 key 前缀，须与网关 {@code AuthorizationReadModelKeys.RESOURCE_PREFIX} 一致。
     */
    public static final String RESOURCE_KEY_PREFIX = "auth:resource:";

    /**
     * 参与运行映射的资源状态：换绑未确认期间必须继续按旧绑定放行（4.9），
     * 因此 {@code PENDING_REBIND} / {@code REBIND_REJECTED} 与 {@code ACTIVE} 一样进入映射。
     */
    private static final List<ResourceStatus> EFFECTIVE_RESOURCE_STATUSES = List.of(
            ResourceStatus.ACTIVE, ResourceStatus.PENDING_REBIND, ResourceStatus.REBIND_REJECTED);

    private final RegistryProperties properties;
    private final IServiceDefinitionRepository serviceRepository;
    private final IServiceReleaseRepository releaseRepository;
    private final IApiResourceRepository resourceRepository;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    public ResourceReadModelProjector(RegistryProperties properties,
                                      IServiceDefinitionRepository serviceRepository,
                                      IServiceReleaseRepository releaseRepository,
                                      IApiResourceRepository resourceRepository,
                                      ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                      ObjectMapper objectMapper) {
        this.properties = properties;
        this.serviceRepository = serviceRepository;
        this.releaseRepository = releaseRepository;
        this.resourceRepository = resourceRepository;
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * 由 PostgreSQL 重建某服务的运行读模型并写入 Redis。
     *
     * <p>调用点必须在注册事务<b>提交之后</b>（见 {@code ResourceRegistryServiceImpl}）：
     * 事务内调用会把一个可能被回滚的中间状态投影出去。</p>
     *
     * @param serviceId 服务标识
     */
    public void project(String serviceId) {
        if (!properties.getReadModel().isEnabled()) {
            log.debug("运行读模型投影已关闭（app.registry.read-model.enabled=false），跳过：serviceId={}", serviceId);
            return;
        }
        if (serviceId == null || serviceId.isBlank()) {
            log.warn("运行读模型投影跳过：serviceId 为空");
            return;
        }
        try {
            String payload = buildPayload(serviceId);
            if (payload == null) {
                return;
            }
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                log.warn("运行读模型未写入：Redis 模板不可用（请确认已引入 spring-boot-starter-data-redis 且配置 spring.data.redis.*）。"
                        + "在恢复前网关的 L2 恒未命中，鉴权将退化为回源。serviceId={}", serviceId);
                return;
            }
            redisTemplate.opsForValue().set(key(serviceId), payload, properties.getReadModel().getTtl());
            log.info("运行读模型已投影：serviceId={}，条目数={}", serviceId, payload.length());
        } catch (Exception e) {
            // 读模型是投影，PG 仍是事实来源。3.8 的 Outbox Publisher 落地后承担重试与补偿。
            log.error("运行读模型投影失败，PG 与 Redis 可能暂时不一致（网关将回源，不影响正确性）：serviceId={}", serviceId, e);
        }
    }

    /**
     * 由 PG 重建读模型 JSON；返回 {@code null} 表示没有可投影的内容。
     */
    private String buildPayload(String serviceId) {
        Optional<ServiceDefinition> service = serviceRepository.findByServiceId(serviceId);
        if (service.isEmpty()) {
            log.warn("运行读模型投影跳过：Auth 侧没有该服务的注册记录，serviceId={}", serviceId);
            return null;
        }

        List<ServiceRelease> candidates = new ArrayList<>();
        for (ServiceRelease release : releaseRepository.findByService_ServiceId(serviceId)) {
            if (release.getStatus() != null && release.getStatus().mayServeTraffic()) {
                candidates.add(release);
            }
        }
        if (candidates.isEmpty()) {
            log.warn("运行读模型投影跳过：该服务没有可能承接流量的 Release（全部已退役），serviceId={}", serviceId);
            return null;
        }

        ServiceRelease defaultRelease = resolveDefaultRelease(service.get(), candidates);
        // 默认 Release 优先，其余按「最近登记」排序，保证多个 Release 绑定同一个键时选取结果确定。
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

        List<Route> protectedRoutes = new ArrayList<>();
        List<AnonymousRoute> anonymousRoutes = new ArrayList<>();
        for (Map.Entry<ResourceKey, String> entry : byKey.entrySet()) {
            ResourceKey key = entry.getKey();
            String permission = entry.getValue();
            if (ResourceVersionCalculator.ANONYMOUS_PERMISSION.equals(permission)) {
                anonymousRoutes.add(new AnonymousRoute(key.method(), key.path()));
            } else {
                protectedRoutes.add(new Route(key.method(), key.path(), permission));
            }
        }

        return objectMapper.writeValueAsString(new ServiceRouteModel(
                defaultRelease.getResourceVersion(), defaultRelease.getServiceVersion(), protectedRoutes, anonymousRoutes));
    }

    /**
     * 合并同一 {@code (method, path)} 的多个绑定：先进先出，后到的不同绑定只告警不覆盖。
     */
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
            // 5.7 规则 2：冲突以默认 Release 为准并告警。默认 Release 在排序中优先，
            // 因此这里保留的值一定来自默认 Release（或列表中最靠前的 Release）。
            log.warn("运行读模型存在绑定冲突，已按默认 Release 取定：serviceId={}, route={}, 采用={}（来自 Release {}），忽略={}（来自 Release {}）",
                    serviceId, key.asText(), existing, defaultRelease.getServiceVersion(),
                    permission, resource.getRelease().getServiceVersion());
        }
    }

    /**
     * 解析默认 Release（5.7 规则 2）：优先取部署系统标记的版本；没有标记时取最近登记的 Release。
     *
     * <p>文档写的是「无标记时取最高版本号的 ACTIVE Release」。这里改用「最近登记」：
     * 版本号没有可靠的比较规则（{@code 1.10.0} 与 {@code 1.9.0} 的字典序是反的），
     * 而「最近登记的 Release」在滚动发布下就是当前正在上线的那个版本，语义等价且判定确定。</p>
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

    private String key(String serviceId) {
        return RESOURCE_KEY_PREFIX + serviceId;
    }

    /**
     * 读模型 JSON 的根结构，字段名与网关解析器一致。
     */
    public record ServiceRouteModel(String resourceVersion,
                                    String releaseVersion,
                                    List<Route> resources,
                                    List<AnonymousRoute> anonymousResources) {
    }

    /**
     * 受保护路由：{@code (method, path) → permission}。
     */
    public record Route(String method, String path, String permission) {
    }

    /**
     * 匿名路由：网关只读取 method/path，网关缓存为「无需登录」条目（3.2）。
     */
    public record AnonymousRoute(String method, String path) {
    }
}
