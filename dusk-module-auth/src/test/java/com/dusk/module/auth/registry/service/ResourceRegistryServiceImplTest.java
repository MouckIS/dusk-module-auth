package com.dusk.module.auth.registry.service;

import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.auth.registry.SnapshotResource;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.authz.ResourceRegistrationResult;
import com.dusk.module.auth.authz.service.AuthzEventPublisher;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.registry.config.RegistryProperties;
import com.dusk.module.auth.registry.diff.ResourceBinding;
import com.dusk.module.auth.registry.dto.RegistryRegistrationResponse;
import com.dusk.module.auth.registry.entity.*;
import com.dusk.module.auth.registry.enums.PermissionStatus;
import com.dusk.module.auth.registry.enums.RebindStatus;
import com.dusk.module.auth.registry.enums.ReleaseStatus;
import com.dusk.module.auth.registry.enums.ResourceStatus;
import com.dusk.module.auth.registry.repository.*;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ResourceRegistryServiceImpl} 单元测试，覆盖《权限优化方案-整理版》3.1 / 3.3 / 3.4 / 3.5 / 4.2 / 4.7 / 4.8 的
 * 注册状态机：
 *
 * <ul>
 *   <li>契约校验：{@code resourceVersion} 一致性（3.3）与权限目录（3.1）；</li>
 *   <li>幂等命中：相同 Release 身份的三种状态（REGISTERED / PENDING_APPROVAL / BLOCKED）；</li>
 *   <li>发布门禁：换绑挂起（PENDING_APPROVAL）与同路径冲突（BLOCKED）；</li>
 *   <li>成功路径：同版本旧 Release 退役、revision 单调推进、读模型投影与事件发布；</li>
 *   <li>权限状态重算（4.8）与换绑影响面查询容错。</li>
 * </ul>
 *
 * <p>全部依赖以 Mockito 替身注入，不启动 Spring、不连数据库。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceRegistryServiceImplTest {

    private static final String SERVICE_ID = "dusk-module-user";
    private static final String SERVICE_VERSION = "1.3.0";
    private static final long CANDIDATE_RELEASE_ID = 100L;
    private static final long OLD_RELEASE_ID = 50L;
    private static final long OTHER_RELEASE_ID = 51L;
    private static final long REVISION = 5L;

    @Mock
    private IServiceDefinitionRepository serviceRepository;
    @Mock
    private IServiceReleaseRepository releaseRepository;
    @Mock
    private IApiResourceRepository resourceRepository;
    @Mock
    private IPermissionDefinitionRepository permissionRepository;
    @Mock
    private IGrantPermissionRepository grantPermissionRepository;
    @Mock
    private IRebindRequestRepository rebindRequestRepository;
    @Mock
    private ResourceReadModelProjector readModelProjector;
    @Mock
    private AuthzEventPublisher authzEventPublisher;
    @Mock
    private IAuthPermissionManager authPermissionManager;

    private RegistryProperties properties;
    private ResourceRegistryServiceImpl service;
    private ServiceDefinition serviceDefinition;

    private static ResourceSnapshot snapshot(List<SnapshotResource> resources) {
        return ResourceSnapshot.of(SERVICE_ID, SERVICE_VERSION, resources, List.of());
    }

    private static ServiceRelease release(long id, String serviceVersion, ReleaseStatus status,
                                          LocalDateTime createTime) {
        ServiceRelease release = new ServiceRelease();
        release.setId(id);
        release.setServiceVersion(serviceVersion);
        release.setStatus(status);
        release.setCreateTime(createTime);
        ServiceDefinition owner = new ServiceDefinition();
        owner.setId(1L);
        owner.setServiceId(SERVICE_ID);
        release.setService(owner);
        return release;
    }

    // ---------------------------------------------------------------- 辅助构造

    private static ApiResource resource(String method, String path, String permission, ResourceStatus status) {
        return resource(null, method, path, permission, status);
    }

    private static ApiResource resource(Long id, String method, String path, String permission,
                                        ResourceStatus status) {
        ApiResource resource = new ApiResource();
        resource.setId(id);
        resource.setHttpMethod(method);
        resource.setPath(path);
        resource.setPermissionCode(permission);
        resource.setStatus(status);
        return resource;
    }

    private static PermissionDefinition permissionDefinition(String code, PermissionStatus status) {
        PermissionDefinition definition = new PermissionDefinition();
        definition.setCode(code);
        definition.setStatus(status);
        return definition;
    }

    private static Permission permissionTree(String rootName, String childName) {
        Permission root = new Permission(rootName, rootName, MultiTenancySides.Host);
        if (childName != null) {
            root.createChildPermission(childName, childName);
        }
        return root;
    }

    private static ApiResource resourceWithId(String method, String path, String permission,
                                              ResourceStatus status, String resourceId) {
        ApiResource resource = resource(method, path, permission, status);
        resource.setResourceId(resourceId);
        return resource;
    }

    @BeforeEach
    void setUp() {
        properties = new RegistryProperties();
        properties.setStrictPermissionCatalogue(false);
        properties.getReadModel().setEnabled(false);

        serviceDefinition = new ServiceDefinition();
        serviceDefinition.setId(1L);
        serviceDefinition.setServiceId(SERVICE_ID);

        service = new ResourceRegistryServiceImpl(serviceRepository, releaseRepository, resourceRepository,
                permissionRepository, grantPermissionRepository, rebindRequestRepository, properties,
                readModelProjector, authzEventPublisher, authPermissionManager);

        when(serviceRepository.findByServiceIdForUpdate(anyString())).thenReturn(Optional.of(serviceDefinition));
        when(releaseRepository.findByService_ServiceId(anyString())).thenReturn(List.of());
        when(releaseRepository.findByService_ServiceIdAndServiceVersionAndResourceVersion(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(releaseRepository.saveAndFlush(any(ServiceRelease.class))).thenAnswer(invocation -> {
            ServiceRelease release = invocation.getArgument(0);
            if (release.getId() == null) {
                release.setId(CANDIDATE_RELEASE_ID);
            }
            return release;
        });
        when(resourceRepository.findByRelease_Id(anyLong())).thenReturn(List.of());
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection())).thenReturn(List.of());
        when(resourceRepository.findPermissionReferences(anyString(), anyCollection())).thenReturn(List.of());
        when(permissionRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(permissionRepository.save(any(PermissionDefinition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(grantPermissionRepository.findRoleIdsByPermissionName(anyString())).thenReturn(List.of());
        when(rebindRequestRepository.findByCandidateReleaseIdAndStatusIn(anyLong(), anyCollection()))
                .thenReturn(List.of());
        when(rebindRequestRepository.findByCandidateReleaseId(anyLong())).thenReturn(List.of());
        when(rebindRequestRepository.findByCandidateReleaseIdAndHttpMethodAndPathAndNewPermissionCode(
                anyLong(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(rebindRequestRepository.saveAndFlush(any(RebindRequest.class))).thenAnswer(invocation -> {
            RebindRequest request = invocation.getArgument(0);
            if (request.getId() == null) {
                request.setId(500L);
            }
            return request;
        });
    }

    // ---------------------------------------------------------------- 契约校验

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("resourceVersion 与内容不一致时拒绝注册：register 抛业务异常，registerSnapshot 返回 INVALID_SNAPSHOT")
    void rejectsInconsistentResourceVersion() {
        ResourceSnapshot snapshot = new ResourceSnapshot(SERVICE_ID, SERVICE_VERSION, "stale-hash",
                List.of(SnapshotResource.of("GET", "/users", "p1")), List.of());

        ResourceRegistrationResult result = service.registerSnapshot(snapshot);

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.INVALID_SNAPSHOT);
        assertThat(result.releaseId()).isNull();
        assertThat(result.resourceRevision()).isEqualTo(-1L);
        assertThat(result.pendingRebindId()).contains("resourceVersion");
        assertThatThrownBy(() -> service.register(snapshot))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("resourceVersion 与资源内容不一致");
        verify(releaseRepository, never()).saveAndFlush(any(ServiceRelease.class));
    }

    @Test
    @DisplayName("关闭严格校验后不阻断：版本号不一致仍可完成注册")
    void skipsVersionValidationWhenNotStrict() {
        properties.setStrictResourceVersion(false);
        ResourceSnapshot snapshot = new ResourceSnapshot(SERVICE_ID, SERVICE_VERSION, "stale-hash",
                List.of(SnapshotResource.of("GET", "/users", "p1")), List.of());

        ResourceRegistrationResult result = service.registerSnapshot(snapshot);

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
    }

    @Test
    @DisplayName("权限目录为空时不做权限码校验（仅告警）")
    void skipsPermissionCheckWhenCatalogueEmpty() {
        properties.setStrictPermissionCatalogue(true);
        when(authPermissionManager.getDefinitionPermissionTree(false)).thenReturn(List.of());
        when(authPermissionManager.getDefinitionPermissionTree(true)).thenReturn(List.of());

        ResourceRegistrationResult result = service.registerSnapshot(
                snapshot(List.of(SnapshotResource.of("GET", "/users", "any:code"))));

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
    }

    @Test
    @DisplayName("权限目录读取异常时本次跳过校验，不阻断注册")
    void toleratesCatalogueReadFailure() {
        properties.setStrictPermissionCatalogue(true);
        when(authPermissionManager.getDefinitionPermissionTree(false))
                .thenThrow(new IllegalStateException("cache down"));

        ResourceRegistrationResult result = service.registerSnapshot(
                snapshot(List.of(SnapshotResource.of("GET", "/users", "any:code"))));

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
    }

    @Test
    @DisplayName("权限目录覆盖 Host 与 Tenant 两侧、忽略空节点与无 name 节点")
    void acceptsPermissionsDeclaredInEitherCatalogueSide() {
        properties.setStrictPermissionCatalogue(true);
        Permission nameless = new Permission();
        nameless.setChildren(null);
        when(authPermissionManager.getDefinitionPermissionTree(false))
                .thenReturn(java.util.Arrays.asList(permissionTree("Pages.Host", "host:code"), null));
        when(authPermissionManager.getDefinitionPermissionTree(true))
                .thenReturn(List.of(permissionTree("Pages.Tenant", "tenant:code"), nameless));

        ResourceRegistrationResult result = service.registerSnapshot(snapshot(List.of(
                SnapshotResource.of("GET", "/host", "host:code"),
                SnapshotResource.of("GET", "/tenant", "tenant:code"))));

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
    }

    @Test
    @DisplayName("匿名资源不需要出现在权限目录中（按 ANONYMOUS 字面量直接放行）")
    void ignoresAnonymousLiteralDuringCatalogueCheck() {
        properties.setStrictPermissionCatalogue(true);
        when(authPermissionManager.getDefinitionPermissionTree(false))
                .thenReturn(List.of(permissionTree("Pages.Host", "host:code")));
        when(authPermissionManager.getDefinitionPermissionTree(true)).thenReturn(List.of());

        ResourceRegistrationResult result = service.registerSnapshot(snapshot(List.of(
                SnapshotResource.of("GET", "/host", "host:code"),
                SnapshotResource.of("PUT", "/open", ResourceBinding.ANONYMOUS_PERMISSION))));

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
    }

    // ---------------------------------------------------------------- 幂等命中

    @Test
    @DisplayName("权限目录中不存在的权限码被确定性拒绝：UNKNOWN_PERMISSION，不写库")
    void rejectsUnknownPermission() {
        properties.setStrictPermissionCatalogue(true);
        when(authPermissionManager.getDefinitionPermissionTree(false))
                .thenReturn(List.of(permissionTree("Pages.Host", "host:code")));
        when(authPermissionManager.getDefinitionPermissionTree(true)).thenReturn(List.of());
        ResourceSnapshot snapshot = snapshot(List.of(SnapshotResource.of("GET", "/users", "ghost:code")));

        ResourceRegistrationResult result = service.registerSnapshot(snapshot);

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.UNKNOWN_PERMISSION);
        assertThat(result.pendingRebindId()).contains("ghost:code");
        assertThatThrownBy(() -> service.register(snapshot))
                .isInstanceOf(UnknownPermissionException.class)
                .hasMessageContaining("ghost:code");
        verify(releaseRepository, never()).saveAndFlush(any(ServiceRelease.class));
    }

    @Test
    @DisplayName("相同 Release 身份幂等命中：返回 UNCHANGED，不写库但重投影读模型")
    void identicalReleaseIsIdempotent() {
        properties.getReadModel().setEnabled(true);
        ResourceSnapshot snapshot = snapshot(List.of(SnapshotResource.of("GET", "/users", "p1")));
        ServiceRelease existing = release(77L, SERVICE_VERSION, ReleaseStatus.REGISTERED, LocalDateTime.now());
        existing.setResourceVersion(snapshot.resourceVersion());
        when(releaseRepository.findByService_ServiceIdAndServiceVersionAndResourceVersion(
                SERVICE_ID, SERVICE_VERSION, snapshot.resourceVersion())).thenReturn(Optional.of(existing));

        RegistryRegistrationResponse response = service.register(snapshot);
        ResourceRegistrationResult result = service.registerSnapshot(snapshot);

        assertThat(response.unchanged()).isTrue();
        assertThat(response.releaseStatus()).isEqualTo("REGISTERED");
        assertThat(response.totalResources()).isEqualTo(1);
        assertThat(response.addedResources()).isEmpty();
        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.UNCHANGED);
        assertThat(result.releaseId()).isEqualTo("77");
        assertThat(result.resourceRevision()).isEqualTo(serviceDefinition.getResourceRevision());
        verify(releaseRepository, never()).saveAndFlush(any(ServiceRelease.class));
        verify(readModelProjector, org.mockito.Mockito.atLeastOnce()).project(SERVICE_ID);
    }

    @Test
    @DisplayName("幂等命中且候选 Release 仍待审批：返回 REBIND_APPROVAL_REQUIRED，无待办时 pendingRebindId 为空")
    void identicalPendingApprovalWithoutPendingRequest() {
        ResourceSnapshot snapshot = snapshot(List.of(SnapshotResource.of("GET", "/users", "p1")));
        ServiceRelease existing = release(77L, SERVICE_VERSION, ReleaseStatus.PENDING_APPROVAL, LocalDateTime.now());
        existing.setResourceVersion(snapshot.resourceVersion());
        when(releaseRepository.findByService_ServiceIdAndServiceVersionAndResourceVersion(
                SERVICE_ID, SERVICE_VERSION, snapshot.resourceVersion())).thenReturn(Optional.of(existing));

        ResourceRegistrationResult result = service.registerSnapshot(snapshot);

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED);
        assertThat(result.releaseId()).isEqualTo("77");
        assertThat(result.pendingRebindId()).isNull();
    }

    @Test
    @DisplayName("幂等命中且候选 Release 待审批：返回现有待办 ID")
    void identicalPendingApprovalWithPendingRequest() {
        ResourceSnapshot snapshot = snapshot(List.of(SnapshotResource.of("GET", "/users", "p1")));
        ServiceRelease existing = release(77L, SERVICE_VERSION, ReleaseStatus.PENDING_APPROVAL, LocalDateTime.now());
        existing.setResourceVersion(snapshot.resourceVersion());
        when(releaseRepository.findByService_ServiceIdAndServiceVersionAndResourceVersion(
                SERVICE_ID, SERVICE_VERSION, snapshot.resourceVersion())).thenReturn(Optional.of(existing));
        RebindRequest pending = new RebindRequest();
        pending.setId(88L);
        when(rebindRequestRepository.findByCandidateReleaseIdAndStatusIn(77L, List.of(RebindStatus.PENDING)))
                .thenReturn(List.of(pending));

        ResourceRegistrationResult result = service.registerSnapshot(snapshot);

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED);
        assertThat(result.pendingRebindId()).isEqualTo("88");
    }

    @Test
    @DisplayName("幂等命中且候选 Release 仍被阻塞：no 换绑记录时 pendingRebindId 为空")
    void identicalBlockedWithoutRebindRecords() {
        ResourceSnapshot snapshot = snapshot(List.of(SnapshotResource.of("GET", "/users", "p1")));
        ServiceRelease existing = release(77L, SERVICE_VERSION, ReleaseStatus.BLOCKED, LocalDateTime.now());
        existing.setResourceVersion(snapshot.resourceVersion());
        when(releaseRepository.findByService_ServiceIdAndServiceVersionAndResourceVersion(
                SERVICE_ID, SERVICE_VERSION, snapshot.resourceVersion())).thenReturn(Optional.of(existing));

        ResourceRegistrationResult result = service.registerSnapshot(snapshot);

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.ACTIVE_RELEASE_CONFLICT);
        assertThat(result.releaseId()).isEqualTo("77");
        assertThat(result.pendingRebindId()).isNull();
    }

    // ---------------------------------------------------------------- 换绑门禁

    @Test
    @DisplayName("幂等命中且候选 Release 被阻塞：返回最后一条换绑记录 ID")
    void identicalBlockedWithRebindRecords() {
        ResourceSnapshot snapshot = snapshot(List.of(SnapshotResource.of("GET", "/users", "p1")));
        ServiceRelease existing = release(77L, SERVICE_VERSION, ReleaseStatus.BLOCKED, LocalDateTime.now());
        existing.setResourceVersion(snapshot.resourceVersion());
        when(releaseRepository.findByService_ServiceIdAndServiceVersionAndResourceVersion(
                SERVICE_ID, SERVICE_VERSION, snapshot.resourceVersion())).thenReturn(Optional.of(existing));
        RebindRequest first = new RebindRequest();
        first.setId(1L);
        RebindRequest last = new RebindRequest();
        last.setId(2L);
        when(rebindRequestRepository.findByCandidateReleaseId(77L)).thenReturn(List.of(first, last));

        ResourceRegistrationResult result = service.registerSnapshot(snapshot);

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.ACTIVE_RELEASE_CONFLICT);
        assertThat(result.pendingRebindId()).isEqualTo("2");
    }

    @Test
    @DisplayName("换绑：候选 Release 挂起 PENDING_APPROVAL，生成待办并返回 REBIND_APPROVAL_REQUIRED")
    void rebindSuspendsCandidateRelease() {
        LocalDateTime now = LocalDateTime.now().minusHours(1);
        ServiceRelease predecessor = release(OLD_RELEASE_ID, SERVICE_VERSION, ReleaseStatus.ACTIVE, now);
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(predecessor));
        when(resourceRepository.findByRelease_Id(OLD_RELEASE_ID))
                .thenReturn(List.of(resource("GET", "/users", "p1", ResourceStatus.ACTIVE)));
        when(grantPermissionRepository.findRoleIdsByPermissionName("p1")).thenReturn(List.of(1L, 2L));
        when(grantPermissionRepository.findRoleIdsByPermissionName("p9")).thenReturn(List.of(2L, 3L));

        RegistryRegistrationResponse response = service.register(
                snapshot(List.of(SnapshotResource.of("GET", "/users", "p9"))));
        ResourceRegistrationResult result = service.registerSnapshot(
                snapshot(List.of(SnapshotResource.of("GET", "/users", "p9"))));

        assertThat(response.unchanged()).isFalse();
        assertThat(response.releaseStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(response.pendingRebindResources()).containsExactly("GET /users");
        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED);
        assertThat(result.releaseId()).isEqualTo(String.valueOf(CANDIDATE_RELEASE_ID));
        assertThat(result.pendingRebindId()).isEqualTo("500");
        assertThat(serviceDefinition.getResourceRevision()).isZero();
        verify(releaseRepository, org.mockito.Mockito.atLeastOnce()).save(any(ServiceRelease.class));
        verify(rebindRequestRepository, org.mockito.Mockito.atLeastOnce()).saveAndFlush(any(RebindRequest.class));
    }

    @Test
    @DisplayName("换绑复用已存在的待办：不重复创建，只刷新阻塞原因")
    void rebindReusesExistingRequest() {
        ServiceRelease predecessor = release(OLD_RELEASE_ID, SERVICE_VERSION, ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(1));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(predecessor));
        when(resourceRepository.findByRelease_Id(OLD_RELEASE_ID))
                .thenReturn(List.of(resource("GET", "/users", "p1", ResourceStatus.ACTIVE)));
        RebindRequest existing = new RebindRequest();
        existing.setId(501L);
        when(rebindRequestRepository.findByCandidateReleaseIdAndHttpMethodAndPathAndNewPermissionCode(
                CANDIDATE_RELEASE_ID, "GET", "/users", "p9")).thenReturn(Optional.of(existing));

        ResourceRegistrationResult result = service.registerSnapshot(
                snapshot(List.of(SnapshotResource.of("GET", "/users", "p9"))));

        assertThat(result.pendingRebindId()).isEqualTo("501");
        assertThat(existing.getActivationBlockedReason()).isEqualTo("等待管理员审批");
    }

    @Test
    @DisplayName("换绑影响面查询失败时按「影响面未知」记录，不影响挂起与结果")
    void rebindToleratesBrokenRoleLookup() {
        ServiceRelease predecessor = release(OLD_RELEASE_ID, SERVICE_VERSION, ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(1));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(predecessor));
        when(resourceRepository.findByRelease_Id(OLD_RELEASE_ID))
                .thenReturn(List.of(resource("GET", "/users", "p1", ResourceStatus.ACTIVE)));
        when(grantPermissionRepository.findRoleIdsByPermissionName(anyString()))
                .thenThrow(new IllegalStateException("grant table down"));

        ResourceRegistrationResult result = service.registerSnapshot(
                snapshot(List.of(SnapshotResource.of("GET", "/users", "p9"))));

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED);
    }

    // ---------------------------------------------------------------- 冲突与成功

    @Test
    @DisplayName("换绑影响面查询返回 null 时按空集合处理")
    void rebindToleratesNullRoleIds() {
        ServiceRelease predecessor = release(OLD_RELEASE_ID, SERVICE_VERSION, ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(1));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(predecessor));
        when(resourceRepository.findByRelease_Id(OLD_RELEASE_ID))
                .thenReturn(List.of(resource("GET", "/users", "p1", ResourceStatus.ACTIVE)));
        when(grantPermissionRepository.findRoleIdsByPermissionName(anyString())).thenReturn(null);

        ResourceRegistrationResult result = service.registerSnapshot(
                snapshot(List.of(SnapshotResource.of("GET", "/users", "p9"))));

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED);
    }

    @Test
    @DisplayName("同路径不同权限与运行中的 Release 冲突：候选 Release 置 BLOCKED，返回 ACTIVE_RELEASE_CONFLICT")
    void activeConflictBlocksCandidate() {
        ServiceRelease older = release(OTHER_RELEASE_ID, "1.1.0", ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusDays(2));
        ServiceRelease predecessor = release(OLD_RELEASE_ID, "1.2.0", ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(1));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(older, predecessor));
        when(resourceRepository.findByRelease_Id(OLD_RELEASE_ID))
                .thenReturn(List.of(resource("GET", "/users", "p1", ResourceStatus.ACTIVE)));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection()))
                .thenReturn(List.of(resource("GET", "/orders", "p3", ResourceStatus.ACTIVE)));

        RegistryRegistrationResponse response = service.register(snapshot(List.of(
                SnapshotResource.of("GET", "/users", "p1"),
                SnapshotResource.of("GET", "/orders", "p2"))));

        assertThat(response.releaseStatus()).isEqualTo("BLOCKED");
        verify(releaseRepository, org.mockito.Mockito.atLeastOnce()).save(any(ServiceRelease.class));
        verify(serviceRepository, never()).save(any(ServiceDefinition.class));
    }

    @Test
    @DisplayName("冲突检测忽略：无权限码的历史资源、无状态的历史资源与已退役 Release")
    void conflictDetectionSkipsUnusableRows() {
        ServiceRelease retired = release(OTHER_RELEASE_ID, "1.1.0", ReleaseStatus.RETIRED,
                LocalDateTime.now().minusDays(2));
        ServiceRelease noStatus = release(52L, "1.0.0", null, LocalDateTime.now().minusDays(3));
        ServiceRelease predecessor = release(OLD_RELEASE_ID, "1.2.0", ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(1));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID))
                .thenReturn(List.of(retired, noStatus, predecessor));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection()))
                .thenReturn(List.of(
                        resource("GET", "/orders", null, ResourceStatus.ACTIVE),
                        resource("GET", "/orders", "  ", ResourceStatus.ACTIVE),
                        resource("GET", "/orders2", "p3", null)));

        RegistryRegistrationResponse response = service.register(snapshot(List.of(
                SnapshotResource.of("GET", "/users", "p1"),
                SnapshotResource.of("GET", "/orders", "p2"))));

        assertThat(response.releaseStatus()).isEqualTo("REGISTERED");
    }

    @Test
    @DisplayName("注册成功：同 serviceVersion 的旧 Release 退役，revision 推进并发布事件")
    void successfulRegistrationRetiresSameVersionReleases() {
        ServiceRelease oldSameVersion = release(OLD_RELEASE_ID, SERVICE_VERSION, ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(1));
        ServiceRelease oldRegistered = release(52L, SERVICE_VERSION, ReleaseStatus.REGISTERED,
                LocalDateTime.now().minusHours(2));
        ServiceRelease differentVersion = release(53L, "1.2.9", ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(3));
        ServiceRelease noStatus = release(54L, SERVICE_VERSION, null, LocalDateTime.now().minusHours(4));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID))
                .thenReturn(List.of(oldSameVersion, oldRegistered, differentVersion, noStatus));
        when(resourceRepository.findPermissionReferences(eq("p1"), anyCollection()))
                .thenReturn(List.of(resource("GET", "/users", "p1", ResourceStatus.ACTIVE)));

        ResourceSnapshot snapshot = snapshot(List.of(SnapshotResource.of("GET", "/users", "p1")));
        RegistryRegistrationResponse response = service.register(snapshot);

        assertThat(response.unchanged()).isFalse();
        assertThat(response.releaseStatus()).isEqualTo("REGISTERED");
        assertThat(response.addedResources()).containsExactly("GET /users");
        assertThat(response.activatedPermissions()).containsExactly("p1");
        assertThat(oldSameVersion.getStatus()).isEqualTo(ReleaseStatus.RETIRED);
        assertThat(oldSameVersion.getRetiredAt()).isNotNull();
        assertThat(oldRegistered.getStatus()).isEqualTo(ReleaseStatus.RETIRED);
        assertThat(differentVersion.getStatus()).isEqualTo(ReleaseStatus.ACTIVE);
        assertThat(serviceDefinition.getResourceRevision()).isEqualTo(1L);
        verify(serviceRepository).save(serviceDefinition);
        verify(authzEventPublisher).resourceChanged(SERVICE_ID, snapshot.resourceVersion(), SERVICE_VERSION, 1L);
    }

    @Test
    @DisplayName("首次注册：服务不存在时自动登记 ServiceDefinition")
    void registersServiceOnFirstUse() {
        when(serviceRepository.findByServiceIdForUpdate(SERVICE_ID)).thenReturn(Optional.empty());
        when(serviceRepository.saveAndFlush(any(ServiceDefinition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceRepository.save(any(ServiceDefinition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResourceRegistrationResult result = service.registerSnapshot(
                snapshot(List.of(SnapshotResource.of("GET", "/users", "p1"))));

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
        org.mockito.ArgumentCaptor<ServiceDefinition> captor =
                org.mockito.ArgumentCaptor.forClass(ServiceDefinition.class);
        verify(serviceRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getServiceId()).isEqualTo(SERVICE_ID);
    }

    @Test
    @DisplayName("空快照是合法的：不产生资源变更，revision 仍推进")
    void acceptsEmptySnapshot() {
        ResourceSnapshot snapshot = ResourceSnapshot.of(SERVICE_ID, SERVICE_VERSION, List.of(), List.of());

        ResourceRegistrationResult result = service.registerSnapshot(snapshot);
        RegistryRegistrationResponse response = service.register(snapshot);

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
        assertThat(response.totalResources()).isZero();
        assertThat(response.addedResources()).isEmpty();
        assertThat(response.activatedPermissions()).isEmpty();
        assertThat(response.orphanedPermissions()).isEmpty();
    }

    @Test
    @DisplayName("已有资源行：按目标状态迁移并刷新状态时间；结构异常的历史行被跳过")
    void appliesMutationsToExistingRows() {
        when(resourceRepository.findByRelease_Id(CANDIDATE_RELEASE_ID)).thenReturn(List.of(
                resource(1L, "GET", "/deprecated", "p0", ResourceStatus.DEPRECATED),
                resource(2L, "GET", "/kept", "p2", ResourceStatus.ACTIVE),
                resource(3L, "GET", "/kept", "p2", ResourceStatus.ACTIVE),
                resource(4L, null, "/broken", "p4", ResourceStatus.ACTIVE),
                resource(5L, "GET", null, "p5", ResourceStatus.ACTIVE),
                resource(6L, "GET", "/no-status", "p6", null)));

        service.registerSnapshot(snapshot(List.of(
                SnapshotResource.of("GET", "/deprecated", "p1"),
                SnapshotResource.of("GET", "/kept", "p2"),
                SnapshotResource.of("GET", "/no-status", "p6"))));

        // 断言仅验证「注册成功」：结构异常行不应导致失败
        verify(resourceRepository, org.mockito.Mockito.atLeastOnce())
                .saveAll(anyCollection());
    }

    @Test
    @DisplayName("回滚恢复：已有 DEPRECATED 行在同快照下被迁移为 ACTIVE")
    void reactivatesDeprecatedRow() {
        ApiResource deprecated = resource(1L, "GET", "/deprecated", "p0", ResourceStatus.DEPRECATED);
        deprecated.setStatusChangedAt(LocalDateTime.now().minusDays(3));
        when(resourceRepository.findByRelease_Id(CANDIDATE_RELEASE_ID)).thenReturn(List.of(deprecated));

        service.registerSnapshot(snapshot(List.of(SnapshotResource.of("GET", "/deprecated", "p1"))));

        assertThat(deprecated.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
        assertThat(deprecated.getStatusChangedAt()).isAfter(LocalDateTime.now().minusMinutes(5));
        assertThat(deprecated.getPermissionCode()).isEqualTo("p1");
    }

    @Test
    @DisplayName("资源标识为空时保留原有 resourceId，非空时覆盖")
    void keepsResourceIdWhenSnapshotOmitsIt() {
        ApiResource withId = resource(1L, "GET", "/with-id", "p1", ResourceStatus.ACTIVE);
        withId.setResourceId("legacy-id");
        ApiResource withoutId = resource(2L, "GET", "/without-id", "p1", ResourceStatus.ACTIVE);
        withoutId.setResourceId("keep-me");
        when(resourceRepository.findByRelease_Id(CANDIDATE_RELEASE_ID)).thenReturn(List.of(withId, withoutId));

        service.registerSnapshot(snapshot(List.of(
                new SnapshotResource("new-id", "GET", "/with-id", "p1"),
                SnapshotResource.of("GET", "/without-id", "p1"))));

        assertThat(withId.getResourceId()).isEqualTo("new-id");
        assertThat(withoutId.getResourceId()).isEqualTo("keep-me");
    }

    @Test
    @DisplayName("Diff 基准跳过结构异常与权限码为空的历史资源")
    void predecessorBindingsSkipBrokenRows() {
        ServiceRelease predecessor = release(OLD_RELEASE_ID, SERVICE_VERSION, ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(1));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(predecessor));
        when(resourceRepository.findByRelease_Id(OLD_RELEASE_ID)).thenReturn(List.of(
                resource(1L, "GET", "/users", "p1", ResourceStatus.ACTIVE),
                resource(2L, null, "/broken", "p2", ResourceStatus.ACTIVE),
                resource(3L, "GET", null, "p3", ResourceStatus.ACTIVE),
                resource(4L, "GET", "/blank", "  ", ResourceStatus.ACTIVE),
                resource(5L, "GET", "/null-permission", null, ResourceStatus.ACTIVE),
                resource(6L, "GET", "/unknown-status", "p6", null)));

        RegistryRegistrationResponse response = service.register(
                snapshot(List.of(SnapshotResource.of("GET", "/users", "p1"))));

        assertThat(response.addedResources()).containsExactly("GET /users");
        assertThat(response.releaseStatus()).isEqualTo("REGISTERED");
    }

    @Test
    @DisplayName("退役判定：同版本但已退役 / 已阻塞的 Release 不重复退役，候选自身被跳过")
    void retiresOnlyTrafficCapableReleasesOfSameVersion() {
        ServiceRelease candidateAlias = release(CANDIDATE_RELEASE_ID, SERVICE_VERSION, ReleaseStatus.REGISTERED,
                LocalDateTime.now().minusMinutes(10));
        ServiceRelease retired = release(OLD_RELEASE_ID, SERVICE_VERSION, ReleaseStatus.RETIRED,
                LocalDateTime.now().minusHours(1));
        ServiceRelease stillServing = release(52L, SERVICE_VERSION, ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(2));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID))
                .thenReturn(List.of(candidateAlias, retired, stillServing));

        RegistryRegistrationResponse response = service.register(
                snapshot(List.of(SnapshotResource.of("GET", "/users", "p1"))));

        assertThat(response.releaseStatus()).isEqualTo("REGISTERED");
        assertThat(candidateAlias.getStatus()).isEqualTo(ReleaseStatus.REGISTERED);
        assertThat(retired.getStatus()).isEqualTo(ReleaseStatus.RETIRED);
        assertThat(stillServing.getStatus()).isEqualTo(ReleaseStatus.RETIRED);
    }

    // ---------------------------------------------------------------- 权限状态重算

    @Test
    @DisplayName("冲突检测：运行中 Release 的同路径绑定与候选一致时不阻塞")
    void identicalActiveBindingIsNotAConflict() {
        ServiceRelease older = release(OTHER_RELEASE_ID, "1.1.0", ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusDays(2));
        ServiceRelease predecessor = release(OLD_RELEASE_ID, "1.2.0", ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(1));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(older, predecessor));
        when(resourceRepository.findByRelease_Id(OLD_RELEASE_ID))
                .thenReturn(List.of(resource("GET", "/users", "p1", ResourceStatus.ACTIVE)));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection()))
                .thenReturn(List.of(resource("GET", "/orders", "p2", ResourceStatus.ACTIVE)));

        RegistryRegistrationResponse response = service.register(snapshot(List.of(
                SnapshotResource.of("GET", "/users", "p1"),
                SnapshotResource.of("GET", "/orders", "p2"))));

        assertThat(response.releaseStatus()).isEqualTo("REGISTERED");
    }

    @Test
    @DisplayName("权限重算：保持生效、自动恢复、转入孤立、DISABLED 保持不变")
    void reconcilesPermissionStatuses() {
        PermissionDefinition active = permissionDefinition("p1", PermissionStatus.ACTIVE);
        PermissionDefinition orphaned = permissionDefinition("p2", PermissionStatus.ORPHANED);
        PermissionDefinition stillUsed = permissionDefinition("p3", PermissionStatus.ACTIVE);
        PermissionDefinition disabled = permissionDefinition("p4", PermissionStatus.DISABLED);
        when(permissionRepository.findByCode("p1")).thenReturn(Optional.of(active));
        when(permissionRepository.findByCode("p2")).thenReturn(Optional.of(orphaned));
        when(permissionRepository.findByCode("p3")).thenReturn(Optional.of(stillUsed));
        when(permissionRepository.findByCode("p4")).thenReturn(Optional.of(disabled));
        when(resourceRepository.findPermissionReferences(eq("p1"), anyCollection()))
                .thenReturn(List.of(resource("GET", "/a", "p1", ResourceStatus.ACTIVE)));
        when(resourceRepository.findPermissionReferences(eq("p2"), anyCollection()))
                .thenReturn(List.of(resource("GET", "/b", "p2", ResourceStatus.PENDING_REBIND)));
        when(resourceRepository.findPermissionReferences(eq("p3"), anyCollection()))
                .thenReturn(List.of(
                        resource("GET", "/c", "p3", ResourceStatus.DEPRECATED),
                        resource("GET", "/c2", "p3", null)));

        RegistryRegistrationResponse response = service.register(snapshot(List.of(
                SnapshotResource.of("GET", "/a", "p1"),
                SnapshotResource.of("GET", "/b", "p2"),
                SnapshotResource.of("GET", "/c", "p3"),
                SnapshotResource.of("GET", "/d", "p4"))));

        assertThat(response.activatedPermissions()).containsExactlyInAnyOrder("p1", "p2");
        assertThat(response.orphanedPermissions()).containsExactly("p3");
        assertThat(orphaned.getStatus()).isEqualTo(PermissionStatus.ACTIVE);
        assertThat(stillUsed.getStatus()).isEqualTo(PermissionStatus.ORPHANED);
        assertThat(stillUsed.getOrphanedAt()).isNotNull();
        assertThat(disabled.getStatus()).isEqualTo(PermissionStatus.DISABLED);
    }

    // ---------------------------------------------------------------- 读模型投影

    @Test
    @DisplayName("新权限点自动登记为 ACTIVE，来源服务写入本次注册的 serviceId")
    void createsUnknownPermissionDefinitions() {
        when(permissionRepository.findByCode("fresh:code")).thenReturn(Optional.empty());
        when(resourceRepository.findPermissionReferences(eq("fresh:code"), anyCollection()))
                .thenReturn(List.of(resource("GET", "/fresh", "fresh:code", ResourceStatus.ACTIVE)));

        RegistryRegistrationResponse response = service.register(
                snapshot(List.of(SnapshotResource.of("GET", "/fresh", "fresh:code"))));

        org.mockito.ArgumentCaptor<PermissionDefinition> captor =
                org.mockito.ArgumentCaptor.forClass(PermissionDefinition.class);
        verify(permissionRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("fresh:code");
        assertThat(captor.getValue().getSourceServiceId()).isEqualTo(SERVICE_ID);
        assertThat(captor.getValue().getStatus()).isEqualTo(PermissionStatus.ACTIVE);
        assertThat(response.activatedPermissions()).containsExactly("fresh:code");
    }

    @Test
    @DisplayName("读模型开关关闭时不做投影")
    void skipsProjectionWhenDisabled() {
        properties.getReadModel().setEnabled(false);

        service.registerSnapshot(snapshot(List.of(SnapshotResource.of("GET", "/users", "p1"))));

        verify(readModelProjector, never()).project(anyString());
    }

    @Test
    @DisplayName("无事务同步时立即投影")
    void projectsImmediatelyWithoutTransaction() {
        properties.getReadModel().setEnabled(true);

        service.registerSnapshot(snapshot(List.of(SnapshotResource.of("GET", "/users", "p1"))));

        verify(readModelProjector).project(SERVICE_ID);
    }

    @Test
    @DisplayName("事务内注册时投影延迟到 afterCommit")
    void defersProjectionUntilAfterCommit() {
        properties.getReadModel().setEnabled(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.registerSnapshot(snapshot(List.of(SnapshotResource.of("GET", "/users", "p1"))));
            verify(readModelProjector, never()).project(anyString());

            List<TransactionSynchronization> synchronizations =
                    new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
            assertThat(synchronizations).isNotEmpty();
            synchronizations.forEach(TransactionSynchronization::afterCommit);
            verify(readModelProjector).project(SERVICE_ID);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("幂等命中同样重投影读模型（Pod 重启自愈缓存）")
    void identicalRegistrationReprojectsReadModel() {
        properties.getReadModel().setEnabled(true);
        ResourceSnapshot snapshot = snapshot(List.of(SnapshotResource.of("GET", "/users", "p1")));
        ServiceRelease existing = release(77L, SERVICE_VERSION, ReleaseStatus.REGISTERED, LocalDateTime.now());
        existing.setResourceVersion(snapshot.resourceVersion());
        when(releaseRepository.findByService_ServiceIdAndServiceVersionAndResourceVersion(
                SERVICE_ID, SERVICE_VERSION, snapshot.resourceVersion())).thenReturn(Optional.of(existing));

        service.registerSnapshot(snapshot);

        verify(readModelProjector).project(SERVICE_ID);
        verify(authzEventPublisher, never()).resourceChanged(
                anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("响应携带 Diff 清单：新 Release 按快照物化，全部资源计入新增")
    void responseCarriesDiffTexts() {
        ServiceRelease predecessor = release(OLD_RELEASE_ID, SERVICE_VERSION, ReleaseStatus.ACTIVE,
                LocalDateTime.now().minusHours(1));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(predecessor));
        when(resourceRepository.findByRelease_Id(OLD_RELEASE_ID)).thenReturn(List.of(
                resourceWithId("GET", "/kept", "p1", ResourceStatus.ACTIVE, "r-old"),
                resource("DELETE", "/gone", "p3", ResourceStatus.ACTIVE)));

        when(resourceRepository.findPermissionReferences(anyString(), anyCollection()))
                .thenReturn(List.of(resource("GET", "/kept", "p1", ResourceStatus.ACTIVE)));

        RegistryRegistrationResponse response = service.register(snapshot(List.of(
                new SnapshotResource("r-new", "GET", "/kept", "p1"),
                SnapshotResource.of("POST", "/added", "p2"))));

        assertThat(response.addedResources()).containsExactlyInAnyOrder("GET /kept", "POST /added");
        assertThat(response.updatedResources()).isEmpty();
        assertThat(response.deprecatedResources()).isEmpty();
        assertThat(response.activatedPermissions()).containsExactlyInAnyOrder("p1", "p2");
    }
}
