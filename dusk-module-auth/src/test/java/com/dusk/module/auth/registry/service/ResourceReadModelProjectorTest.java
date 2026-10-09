package com.dusk.module.auth.registry.service;

import com.dusk.module.auth.registry.config.RegistryProperties;
import com.dusk.module.auth.registry.entity.ApiResource;
import com.dusk.module.auth.registry.entity.ServiceDefinition;
import com.dusk.module.auth.registry.entity.ServiceRelease;
import com.dusk.module.auth.registry.enums.ReleaseStatus;
import com.dusk.module.auth.registry.enums.ResourceStatus;
import com.dusk.module.auth.registry.repository.IApiResourceRepository;
import com.dusk.module.auth.registry.repository.IServiceDefinitionRepository;
import com.dusk.module.auth.registry.repository.IServiceReleaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ResourceReadModelProjector} 单元测试。
 *
 * <p>两个必守的点：① 写出的 JSON 必须与网关解析器（{@code AuthorizationReadModelJson}）的字段契约一致，
 * 否则网关会把整份快照判为解析失败并退化回源；② 投影失败绝不能向上抛——
 * 注册事务已经提交，缓存写失败不该把一次成功的注册变成失败。</p>
 */
@ExtendWith(MockitoExtension.class)
class ResourceReadModelProjectorTest {

    private static final String SERVICE_ID = "dusk-module-user";

    @Mock
    private IServiceDefinitionRepository serviceRepository;
    @Mock
    private IServiceReleaseRepository releaseRepository;
    @Mock
    private IApiResourceRepository resourceRepository;
    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RegistryProperties properties;
    private ResourceReadModelProjector projector;

    @BeforeEach
    void setUp() {
        properties = new RegistryProperties();
        projector = new ResourceReadModelProjector(properties, serviceRepository, releaseRepository,
                resourceRepository, redisTemplateProvider, objectMapper);
    }

    @Test
    @DisplayName("写出 auth:resource:{serviceId}，字段与网关解析器契约一致")
    void shouldProjectGatewayCompatibleJson() {
        ServiceDefinition service = new ServiceDefinition();
        service.setServiceId(SERVICE_ID);
        ServiceRelease release = release(10L, "1.0.0", "rv-1", ReleaseStatus.REGISTERED);

        when(serviceRepository.findByServiceId(SERVICE_ID)).thenReturn(Optional.of(service));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(release));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection()))
                .thenReturn(List.of(
                        resource(release, "GET", "/users", "system:user:query"),
                        resource(release, "POST", "/login", "ANONYMOUS")));
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        projector.project(SERVICE_ID);

        org.mockito.ArgumentCaptor<String> json = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("auth:resource:" + SERVICE_ID), json.capture(), eq(Duration.ofMinutes(30)));

        JsonNode root = objectMapper.readTree(json.getValue());
        assertThat(root.get("resourceVersion").asString()).isEqualTo("rv-1");
        assertThat(root.get("releaseVersion").asString()).isEqualTo("1.0.0");
        assertThat(root.get("resources").size()).isEqualTo(1);
        assertThat(root.get("resources").get(0).get("method").asString()).isEqualTo("GET");
        assertThat(root.get("resources").get(0).get("path").asString()).isEqualTo("/users");
        assertThat(root.get("resources").get(0).get("permission").asString()).isEqualTo("system:user:query");
        assertThat(root.get("anonymousResources").size()).isEqualTo(1);
        assertThat(root.get("anonymousResources").get(0).get("method").asString()).isEqualTo("POST");
        assertThat(root.get("anonymousResources").get(0).get("path").asString()).isEqualTo("/login");
    }

    @Test
    @DisplayName("关闭投影开关时不做任何读写")
    void shouldSkipWhenDisabled() {
        properties.getReadModel().setEnabled(false);

        projector.project(SERVICE_ID);

        verifyNoInteractions(serviceRepository, releaseRepository, resourceRepository, redisTemplateProvider);
    }

    @Test
    @DisplayName("Redis 不可用只告警，不影响调用方")
    void shouldNotFailWhenRedisUnavailable() {
        ServiceDefinition service = new ServiceDefinition();
        service.setServiceId(SERVICE_ID);
        ServiceRelease release = release(10L, "1.0.0", "rv-1", ReleaseStatus.ACTIVE);

        when(serviceRepository.findByServiceId(SERVICE_ID)).thenReturn(Optional.of(service));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(release));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection())).thenReturn(List.of());
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);

        assertThatCode(() -> projector.project(SERVICE_ID)).doesNotThrowAnyException();
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("写 Redis 抛异常被吞掉：注册事务已提交，缓存失败不能反向影响注册结果")
    void shouldSwallowRedisFailure() {
        ServiceDefinition service = new ServiceDefinition();
        service.setServiceId(SERVICE_ID);
        ServiceRelease release = release(10L, "1.0.0", "rv-1", ReleaseStatus.ACTIVE);

        when(serviceRepository.findByServiceId(SERVICE_ID)).thenReturn(Optional.of(service));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(release));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection())).thenReturn(List.of());
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("Redis 连接不可用"));

        assertThatCode(() -> projector.project(SERVICE_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("无可能承接流量的 Release 时不写入（全部已退役）")
    void shouldSkipWhenNoTrafficCapableRelease() {
        ServiceDefinition service = new ServiceDefinition();
        service.setServiceId(SERVICE_ID);
        when(serviceRepository.findByServiceId(SERVICE_ID)).thenReturn(Optional.of(service));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(
                release(10L, "1.0.0", "rv-1", ReleaseStatus.RETIRED)));

        projector.project(SERVICE_ID);

        verify(redisTemplateProvider, never()).getIfAvailable();
    }

    private static ServiceRelease release(Long id, String version, String resourceVersion, ReleaseStatus status) {
        ServiceRelease release = new ServiceRelease();
        release.setId(id);
        release.setServiceVersion(version);
        release.setResourceVersion(resourceVersion);
        release.setStatus(status);
        release.setCreateTime(LocalDateTime.now());
        return release;
    }

    private static ApiResource resource(ServiceRelease release, String method, String path, String permission) {
        ApiResource resource = new ApiResource();
        resource.setId(1L);
        resource.setRelease(release);
        resource.setHttpMethod(method);
        resource.setPath(path);
        resource.setPermissionCode(permission);
        resource.setStatus(ResourceStatus.ACTIVE);
        return resource;
    }
}
