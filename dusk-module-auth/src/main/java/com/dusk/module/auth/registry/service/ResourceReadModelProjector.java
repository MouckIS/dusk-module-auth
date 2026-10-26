package com.dusk.module.auth.registry.service;

import com.dusk.common.rpc.authz.ResourceRuntimeSnapshot;
import com.dusk.module.auth.registry.config.RegistryProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

/**
 * Redis 运行态读模型投影：{@code auth:resource:{serviceId}}，见《权限优化方案-整理版》5.2 / 6.2。
 *
 * <p>本类只从 PostgreSQL 重建，不做增量拼接。写入使用 Lua 比较 {@code resourceRevision}：
 * 只有严格更大的修订号才能覆盖，旧快照/旧事件永远不会把新读模型回滚（红线⑨、6.2）。</p>
 *
 * <p><b>投影失败必须返回 false</b>：Outbox 只有在投影成功后才允许发送变更事件，
 * 否则网关会先收到「新数据已可见」的提示、回源却读到旧值。注册链路调用本类时
 * 可以忽略返回值——PG 已提交，投影会由 Outbox 的重试与全量重建补偿。</p>
 */
@Component
@Slf4j
public class ResourceReadModelProjector {

    /**
     * 读模型 key 前缀，须与网关 {@code AuthorizationReadModelKeys.RESOURCE_PREFIX} 一致。
     */
    public static final String RESOURCE_KEY_PREFIX = "auth:resource:";

    /**
     * 版本字段名，与网关 JSON 解析器一致。
     */
    public static final String RESOURCE_REVISION_FIELD = "resourceRevision";

    private static final String CAS_WRITE_SCRIPT = """
            local field = ARGV[1]
            local incoming = tonumber(ARGV[2])
            local payload = ARGV[3]
            local ttl = tonumber(ARGV[4])
            local current = redis.call('GET', KEYS[1])
            if current then
              local ok, decoded = pcall(cjson.decode, current)
              if ok and decoded[field] and tonumber(decoded[field]) > incoming then
                return 0
              end
            end
            redis.call('SET', KEYS[1], payload, 'PX', ttl)
            return 1
            """;

    private static final RedisScript<Long> CAS_WRITE =
            new DefaultRedisScript<>(CAS_WRITE_SCRIPT, Long.class);

    private final RegistryProperties properties;
    private final ResourceRuntimeResolver resolver;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    public ResourceReadModelProjector(RegistryProperties properties,
                                      ResourceRuntimeResolver resolver,
                                      ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                      ObjectMapper objectMapper) {
        this.properties = properties;
        this.resolver = resolver;
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * 兼容旧构造方式（单元测试直接注入仓库）。新代码请使用解析器版本，避免规则分叉。
     */
    public ResourceReadModelProjector(
            RegistryProperties properties,
            com.dusk.module.auth.registry.repository.IServiceDefinitionRepository serviceRepository,
            com.dusk.module.auth.registry.repository.IServiceReleaseRepository releaseRepository,
            com.dusk.module.auth.registry.repository.IApiResourceRepository resourceRepository,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper) {
        this(properties, new ResourceRuntimeResolver(serviceRepository, releaseRepository, resourceRepository),
                redisTemplateProvider, objectMapper);
    }

    /**
     * 由 PostgreSQL 重建某服务的运行读模型并写入 Redis。
     *
     * @return {@code true} 表示已写入（或本开关关闭时无需写入视为成功）；{@code false} 表示投影失败，
     * 调用方（Outbox）不得发送对应变更事件
     */
    public boolean project(String serviceId) {
        if (!properties.getReadModel().isEnabled()) {
            log.debug("运行读模型投影已关闭（app.registry.read-model.enabled=false），跳过：serviceId={}", serviceId);
            return true;
        }
        if (serviceId == null || serviceId.isBlank()) {
            log.warn("运行读模型投影跳过：serviceId 为空");
            return false;
        }
        try {
            Optional<ResourceRuntimeSnapshot> snapshot = resolver.resolve(serviceId);
            if (snapshot.isEmpty()) {
                StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
                if (redisTemplate == null) {
                    log.warn("运行读模型未清理：Redis 模板不可用，serviceId={}", serviceId);
                    return false;
                }
                redisTemplate.delete(key(serviceId));
                log.info("服务已无有效资源并集，删除运行读模型：serviceId={}", serviceId);
                return true;
            }
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                log.warn("运行读模型未写入：Redis 模板不可用（请确认已引入 spring-boot-starter-data-redis 且配置 spring.data.redis.*）。"
                        + "在恢复前网关的 L2 恒未命中，鉴权将退化为回源。serviceId={}", serviceId);
                return false;
            }
            String payload = objectMapper.writeValueAsString(toModel(serviceId, snapshot.get()));
            Long written = redisTemplate.execute(CAS_WRITE, List.of(key(serviceId)),
                    RESOURCE_REVISION_FIELD,
                    String.valueOf(snapshot.get().resourceRevision()),
                    payload,
                    String.valueOf(properties.getReadModel().getTtl().toMillis()));
            if (written == null || written == 0L) {
                log.debug("运行读模型存在更新版本，跳过本次写入：serviceId={}, revision={}",
                        serviceId, snapshot.get().resourceRevision());
                return true;
            }
            log.info("运行读模型已投影：serviceId={}, revision={}, 路由数={}",
                    serviceId, snapshot.get().resourceRevision(), snapshot.get().resources().size());
            return true;
        } catch (Exception e) {
            log.error("运行读模型投影失败，PG 与 Redis 可能暂时不一致（Outbox 将重试）：serviceId={}", serviceId, e);
            return false;
        }
    }

    private ServiceRouteModel toModel(String serviceId, ResourceRuntimeSnapshot snapshot) {
        List<Route> routes = snapshot.resources().stream()
                .map(route -> new Route(route.method(), route.path(), route.permission()))
                .toList();
        List<AnonymousRoute> anonymous = snapshot.anonymousResources().stream()
                .map(route -> new AnonymousRoute(route.method(), route.path()))
                .toList();
        return new ServiceRouteModel(serviceId, snapshot.resourceVersion(), snapshot.releaseVersion(),
                snapshot.resourceRevision(), routes, anonymous);
    }

    private String key(String serviceId) {
        return RESOURCE_KEY_PREFIX + serviceId;
    }

    /**
     * 读模型 JSON 的根结构，字段名与网关解析器一致。
     */
    public record ServiceRouteModel(String serviceId,
                                    String resourceVersion,
                                    String releaseVersion,
                                    long resourceRevision,
                                    List<Route> resources,
                                    List<AnonymousRoute> anonymousResources) {
    }

    /**
     * 受保护路由：{@code (method, path) → permission}。
     */
    public record Route(String method, String path, String permission) {
    }

    /**
     * 匿名路由。
     */
    public record AnonymousRoute(String method, String path) {
    }
}
