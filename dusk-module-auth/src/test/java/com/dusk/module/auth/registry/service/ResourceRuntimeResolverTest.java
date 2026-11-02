package com.dusk.module.auth.registry.service;

import com.dusk.common.core.auth.registry.ResourceVersionCalculator;
import com.dusk.common.rpc.authz.ResourceRuntimeSnapshot;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ResourceRuntimeResolver} 单元测试，覆盖《权限优化方案-整理版》4.5 / 5.2 / 5.7：
 * 空输入、未登记服务、无流量 Release、默认 Release 解析、多 Release 并集与冲突仲裁、
 * 空权限码容错。全部使用内存实体与 Mockito，不启动 Spring。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceRuntimeResolverTest {

    private static final String SERVICE_ID = "dusk-module-user";

    @Mock
    private IServiceDefinitionRepository serviceRepository;
    @Mock
    private IServiceReleaseRepository releaseRepository;
    @Mock
    private IApiResourceRepository resourceRepository;

    private ResourceRuntimeResolver resolver;

    private static ServiceDefinition service(String defaultReleaseVersion, long revision) {
        ServiceDefinition definition = new ServiceDefinition();
        definition.setServiceId(SERVICE_ID);
        definition.setDefaultReleaseVersion(defaultReleaseVersion);
        definition.setResourceRevision(revision);
        return definition;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static ServiceRelease release(Long id, String version, ReleaseStatus status,
                                          LocalDateTime createTime, String resourceVersion) {
        ServiceRelease release = new ServiceRelease();
        release.setId(id);
        release.setServiceVersion(version);
        release.setStatus(status);
        release.setCreateTime(createTime);
        release.setResourceVersion(resourceVersion);
        return release;
    }

    private static ApiResource resource(Long id, ServiceRelease release, String method,
                                        String path, String permission) {
        ApiResource resource = new ApiResource();
        resource.setId(id);
        resource.setRelease(release);
        resource.setHttpMethod(method);
        resource.setPath(path);
        resource.setPermissionCode(permission);
        resource.setStatus(ResourceStatus.ACTIVE);
        return resource;
    }

    @BeforeEach
    void setUp() {
        resolver = new ResourceRuntimeResolver(serviceRepository, releaseRepository, resourceRepository);
    }

    private void stubReleases(ServiceDefinition definition, List<ServiceRelease> releases) {
        when(serviceRepository.findByServiceId(SERVICE_ID)).thenReturn(Optional.of(definition));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(releases);
    }

    // ------------------------------------------------------------------
    // 入参与登记校验
    // ------------------------------------------------------------------

    @Test
    @DisplayName("resolve：serviceId 为 null 时返回空")
    void resolve_nullServiceId() {
        assertThat(resolver.resolve(null)).isEmpty();
    }

    @Test
    @DisplayName("resolve：serviceId 为空白时返回空")
    void resolve_blankServiceId() {
        assertThat(resolver.resolve("   ")).isEmpty();
    }

    @Test
    @DisplayName("resolve：服务未登记时返回空")
    void resolve_serviceNotRegistered() {
        when(serviceRepository.findByServiceId(SERVICE_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(SERVICE_ID)).isEmpty();
        verify(serviceRepository).findByServiceId(SERVICE_ID);
    }

    @Test
    @DisplayName("resolve：没有任何可能承接流量的 Release 时返回空")
    void resolve_noTrafficCapableRelease() {
        ServiceDefinition definition = service(null, 1L);
        ServiceRelease nullStatus = release(1L, "1.0.0", null, LocalDateTime.of(2024, 1, 1, 0, 0), "rv1");
        ServiceRelease retired = release(2L, "1.0.0", ReleaseStatus.RETIRED, LocalDateTime.of(2024, 1, 2, 0, 0), "rv2");
        ServiceRelease pending = release(3L, "1.0.0", ReleaseStatus.PENDING_APPROVAL,
                LocalDateTime.of(2024, 1, 3, 0, 0), "rv3");
        when(serviceRepository.findByServiceId(SERVICE_ID)).thenReturn(Optional.of(definition));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID))
                .thenReturn(List.of(nullStatus, retired, pending));

        assertThat(resolver.resolve(SERVICE_ID)).isEmpty();
    }

    // ------------------------------------------------------------------
    // 默认 Release 解析
    // ------------------------------------------------------------------

    @Test
    @DisplayName("resolve：优先使用部署系统标记的默认 Release，并按默认 Release 仲裁同路径冲突")
    void resolve_prefersMarkedDefaultRelease() {
        ServiceDefinition definition = service("2.0.0", 9L);
        ServiceRelease marked = release(2L, "2.0.0", ReleaseStatus.ACTIVE,
                LocalDateTime.of(2024, 1, 1, 0, 0), "rv2");
        ServiceRelease older = release(1L, "1.0.0", ReleaseStatus.ACTIVE,
                LocalDateTime.of(2024, 1, 2, 0, 0), "rv1");
        stubReleases(definition, List.of(older, marked));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection()))
                .thenReturn(List.of(
                        resource(10L, older, "GET", "/a", "P1"),
                        resource(11L, older, "GET", "/b", "P1"),
                        resource(12L, older, "GET", "/c", "P9"),
                        resource(20L, marked, "GET", "/a", "P2"),
                        resource(21L, marked, "GET", "/c", "P9"),
                        resource(22L, marked, "POST", "/anon", ResourceVersionCalculator.ANONYMOUS_PERMISSION),
                        resource(23L, marked, "GET", "/blank", "   "),
                        resource(24L, marked, "GET", "/null", null)));

        ResourceRuntimeSnapshot snapshot = resolver.resolve(SERVICE_ID).orElseThrow();

        assertThat(snapshot.serviceId()).isEqualTo(SERVICE_ID);
        assertThat(snapshot.resourceVersion()).isEqualTo("rv2");
        assertThat(snapshot.releaseVersion()).isEqualTo("2.0.0");
        assertThat(snapshot.resourceRevision()).isEqualTo(9L);
        // 默认 Release 优先：/a 采用 P2，旧 Release 的 P1 被忽略；/c 两侧一致。
        assertThat(snapshot.resources())
                .extracting(ResourceRuntimeSnapshot.RuntimeRoute::method, ResourceRuntimeSnapshot.RuntimeRoute::path,
                        ResourceRuntimeSnapshot.RuntimeRoute::permission)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("GET", "/a", "P2"),
                        org.assertj.core.groups.Tuple.tuple("GET", "/b", "P1"),
                        org.assertj.core.groups.Tuple.tuple("GET", "/c", "P9"));
        assertThat(snapshot.anonymousResources())
                .extracting(ResourceRuntimeSnapshot.RuntimeAnonymousResource::method,
                        ResourceRuntimeSnapshot.RuntimeAnonymousResource::path)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("POST", "/anon"));
    }

    @Test
    @DisplayName("resolve：标记版本不在候选 Release 中时回退为最近登记的 Release")
    void resolve_markedVersionMissingFallsBackToLatest() {
        ServiceDefinition definition = service("9.9.9", 3L);
        ServiceRelease older = release(1L, "1.0.0", ReleaseStatus.ACTIVE,
                LocalDateTime.of(2024, 1, 1, 0, 0), "rv1");
        ServiceRelease latest = release(2L, "2.0.0", ReleaseStatus.ACTIVE,
                LocalDateTime.of(2024, 1, 2, 0, 0), "rv2");
        stubReleases(definition, List.of(older, latest));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection())).thenReturn(List.of());

        ResourceRuntimeSnapshot snapshot = resolver.resolve(SERVICE_ID).orElseThrow();

        assertThat(snapshot.releaseVersion()).isEqualTo("2.0.0");
        assertThat(snapshot.resourceVersion()).isEqualTo("rv2");
        assertThat(snapshot.resources()).isEmpty();
        assertThat(snapshot.anonymousResources()).isEmpty();
    }

    @Test
    @DisplayName("resolve：默认版本为空白时回退，createTime 为空视为最早")
    void resolve_blankMarkedVersionFallsBackWithNullCreateTime() {
        ServiceDefinition definition = service("   ", 4L);
        ServiceRelease nullTime = release(1L, "1.0.0", ReleaseStatus.ACTIVE, null, "rv1");
        ServiceRelease latest = release(2L, "2.0.0", ReleaseStatus.ACTIVE,
                LocalDateTime.of(2024, 1, 2, 0, 0), "rv2");
        stubReleases(definition, List.of(nullTime, latest));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection())).thenReturn(List.of());

        assertThat(resolver.resolve(SERVICE_ID).orElseThrow().releaseVersion()).isEqualTo("2.0.0");
    }

    @Test
    @DisplayName("resolve：默认版本为 null 且 createTime 相同时按 ID 取最大")
    void resolve_nullMarkedVersionTieBreaksById() {
        ServiceDefinition definition = service(null, 1L);
        LocalDateTime sameTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        ServiceRelease lowId = release(1L, "1.0.0", ReleaseStatus.ACTIVE, sameTime, "rv1");
        ServiceRelease highId = release(2L, "2.0.0", ReleaseStatus.ACTIVE, sameTime, "rv2");
        stubReleases(definition, List.of(highId, lowId));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection())).thenReturn(List.of());

        assertThat(resolver.resolve(SERVICE_ID).orElseThrow().releaseVersion()).isEqualTo("2.0.0");
    }

    @Test
    @DisplayName("resolve：默认 Release 的 resourceVersion 为空时输出空串而不是 null")
    void resolve_nullResourceVersion() {
        ServiceDefinition definition = service("1.0.0", 2L);
        ServiceRelease release = release(1L, "1.0.0", ReleaseStatus.REGISTERED,
                LocalDateTime.of(2024, 1, 1, 0, 0), null);
        stubReleases(definition, List.of(release));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection())).thenReturn(List.of());

        ResourceRuntimeSnapshot snapshot = resolver.resolve(SERVICE_ID).orElseThrow();

        assertThat(snapshot.resourceVersion()).isEmpty();
        assertThat(snapshot.releaseVersion()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("resolve：同一路径仅一个 Release 声明时按该绑定输出")
    void resolve_singleRoute() {
        ServiceDefinition definition = service("1.0.0", 5L);
        ServiceRelease release = release(1L, "1.0.0", ReleaseStatus.ACTIVE,
                LocalDateTime.of(2024, 1, 1, 0, 0), "rv1");
        stubReleases(definition, List.of(release));
        when(resourceRepository.findByRelease_IdInAndStatusIn(eq(List.of(1L)), anyCollection()))
                .thenReturn(List.of(resource(10L, release, "GET", "/users", "system:user:query")));

        ResourceRuntimeSnapshot snapshot = resolver.resolve(SERVICE_ID).orElseThrow();

        assertThat(snapshot.resources()).hasSize(1);
        assertThat(snapshot.resources().getFirst().permission()).isEqualTo("system:user:query");
        assertThat(snapshot.anonymousResources()).isEmpty();
    }
}
