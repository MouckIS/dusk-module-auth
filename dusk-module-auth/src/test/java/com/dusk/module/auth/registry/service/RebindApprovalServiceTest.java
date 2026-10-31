package com.dusk.module.auth.registry.service;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.authz.ResourceRegistrationResult;
import com.dusk.module.auth.authz.service.AuthzEventPublisher;
import com.dusk.module.auth.registry.entity.ApiResource;
import com.dusk.module.auth.registry.entity.PermissionDefinition;
import com.dusk.module.auth.registry.entity.RebindRequest;
import com.dusk.module.auth.registry.entity.ServiceDefinition;
import com.dusk.module.auth.registry.entity.ServiceRelease;
import com.dusk.module.auth.registry.enums.PermissionStatus;
import com.dusk.module.auth.registry.enums.RebindStatus;
import com.dusk.module.auth.registry.enums.ReleaseStatus;
import com.dusk.module.auth.registry.enums.ResourceStatus;
import com.dusk.module.auth.registry.repository.IApiResourceRepository;
import com.dusk.module.auth.registry.repository.IPermissionDefinitionRepository;
import com.dusk.module.auth.registry.repository.IRebindRequestRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link RebindApprovalService} 单元测试，覆盖《权限优化方案-整理版》4.7 的换绑审批闭环：
 * 通过 / 拒绝 / 幂等重放 / 仍有待办 / 已拒绝待办 / ACTIVE 冲突 / 资源缺失 / 权限状态重算。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RebindApprovalServiceTest {

    private static final Long RELEASE_ID = 10L;
    private static final String SERVICE_ID = "dusk-module-user";

    @Mock
    private IRebindRequestRepository rebindRequestRepository;
    @Mock
    private IServiceReleaseRepository releaseRepository;
    @Mock
    private IServiceDefinitionRepository serviceRepository;
    @Mock
    private IApiResourceRepository resourceRepository;
    @Mock
    private IPermissionDefinitionRepository permissionRepository;
    @Mock
    private AuthzEventPublisher authzEventPublisher;

    private RebindApprovalService service;

    private static ServiceDefinition definition(long revision) {
        ServiceDefinition definition = new ServiceDefinition();
        definition.setServiceId(SERVICE_ID);
        definition.setResourceRevision(revision);
        return definition;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static ServiceRelease release(Long id, ServiceDefinition service, String version,
                                          ReleaseStatus status, String resourceVersion) {
        ServiceRelease release = new ServiceRelease();
        release.setId(id);
        release.setService(service);
        release.setServiceVersion(version);
        release.setStatus(status);
        release.setResourceVersion(resourceVersion);
        return release;
    }

    private static RebindRequest rebind(Long id, String method, String path, String oldCode,
                                        String newCode, RebindStatus status) {
        RebindRequest request = new RebindRequest();
        request.setId(id);
        request.setCandidateReleaseId(RELEASE_ID);
        request.setHttpMethod(method);
        request.setPath(path);
        request.setOldPermissionCode(oldCode);
        request.setNewPermissionCode(newCode);
        request.setStatus(status);
        return request;
    }

    private static ApiResource resource(Long id, ServiceRelease release, String method, String path,
                                        String permission, ResourceStatus status) {
        ApiResource resource = new ApiResource();
        resource.setId(id);
        resource.setRelease(release);
        resource.setHttpMethod(method);
        resource.setPath(path);
        resource.setPermissionCode(permission);
        resource.setStatus(status);
        return resource;
    }

    private static PermissionDefinition permission(String code, PermissionStatus status) {
        PermissionDefinition definition = new PermissionDefinition();
        definition.setCode(code);
        definition.setStatus(status);
        return definition;
    }

    @BeforeEach
    void setUp() {
        service = new RebindApprovalService(rebindRequestRepository, releaseRepository, serviceRepository,
                resourceRepository, permissionRepository, authzEventPublisher);
        when(permissionRepository.save(any(PermissionDefinition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubStatusQuery(RebindStatus status, List<RebindRequest> result) {
        when(rebindRequestRepository.findByCandidateReleaseIdAndStatusIn(eq(RELEASE_ID), eq(List.of(status))))
                .thenReturn(result);
    }

    // ------------------------------------------------------------------
    // approve：前置校验与幂等
    // ------------------------------------------------------------------

    @Test
    @DisplayName("approve：待办不存在时抛出业务异常")
    void approve_requestMissing() {
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(1L, "admin", "ok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("换绑待办不存在");
        verifyNoInteractions(releaseRepository);
    }

    @Test
    @DisplayName("approve：候选 Release 不存在时抛出业务异常")
    void approve_releaseMissing() {
        when(rebindRequestRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING)));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(1L, "admin", "ok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("候选 Release 不存在");
    }

    @Test
    @DisplayName("approve：已批准的待办重放返回幂等命中，不写库")
    void approve_alreadyApproved() {
        ServiceDefinition definition = definition(7L);
        ServiceRelease release = release(RELEASE_ID, definition, "1.0.0", ReleaseStatus.ACTIVE, "rv1");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.APPROVED);
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));

        RebindApprovalService.RebindDecision decision = service.approve(1L, "admin", "ok");

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
        assertThat(decision.releaseId()).isEqualTo("10");
        assertThat(decision.resourceRevision()).isEqualTo(7L);
        assertThat(decision.pendingRebindId()).isNull();
        assertThat(decision.message()).contains("幂等");
        verify(rebindRequestRepository, never()).save(any());
        verify(releaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("approve：已拒绝的待办不能直接批准")
    void approve_alreadyRejected() {
        ServiceDefinition definition = definition(1L);
        ServiceRelease release = release(RELEASE_ID, definition, "1.0.0", ReleaseStatus.BLOCKED, "rv1");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.REJECTED);
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));

        assertThatThrownBy(() -> service.approve(1L, "admin", "ok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被拒绝");
        verify(releaseRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // approve：仍有待办 / 已拒绝待办
    // ------------------------------------------------------------------

    @Test
    @DisplayName("approve：仍有其他待办时候选 Release 保持待审批")
    void approve_stillHasPendingRequests() {
        ServiceDefinition definition = definition(3L);
        ServiceRelease release = release(RELEASE_ID, definition, "1.0.0", ReleaseStatus.PENDING_APPROVAL, "rv1");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING);
        RebindRequest next = rebind(2L, "POST", "/users", "P2", "P3", RebindStatus.PENDING);
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));
        stubStatusQuery(RebindStatus.PENDING, List.of(next));

        RebindApprovalService.RebindDecision decision = service.approve(1L, "admin", "同意");

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED);
        assertThat(decision.pendingRebindId()).isEqualTo("2");
        assertThat(decision.resourceRevision()).isEqualTo(3L);
        assertThat(release.getStatus()).isEqualTo(ReleaseStatus.PENDING_APPROVAL);
        assertThat(request.getStatus()).isEqualTo(RebindStatus.APPROVED);
        assertThat(request.getDecidedBy()).isEqualTo("admin");
        assertThat(request.getDecisionReason()).isEqualTo("同意");
        verify(releaseRepository).save(release);
    }

    @Test
    @DisplayName("approve：存在已拒绝的待办时候选 Release 被阻塞")
    void approve_hasRejectedRequests() {
        ServiceDefinition definition = definition(4L);
        ServiceRelease release = release(RELEASE_ID, definition, "1.0.0", ReleaseStatus.PENDING_APPROVAL, "rv1");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING);
        RebindRequest rejected = rebind(3L, "DELETE", "/users", "P1", "P2", RebindStatus.REJECTED);
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));
        stubStatusQuery(RebindStatus.PENDING, List.of());
        stubStatusQuery(RebindStatus.REJECTED, List.of(rejected));

        RebindApprovalService.RebindDecision decision = service.approve(1L, "admin", "同意");

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.ACTIVE_RELEASE_CONFLICT);
        assertThat(decision.pendingRebindId()).isEqualTo("3");
        assertThat(release.getStatus()).isEqualTo(ReleaseStatus.BLOCKED);
        assertThat(release.getActivationBlockedReason()).isEqualTo("存在已拒绝的换绑待办");
        verify(releaseRepository).save(release);
    }

    @Test
    @DisplayName("approve：待办对应的资源缺失时抛出业务异常")
    void approve_resourceMissing() {
        ServiceDefinition definition = definition(4L);
        ServiceRelease release = release(RELEASE_ID, definition, "1.0.0", ReleaseStatus.PENDING_APPROVAL, "rv1");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING);
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));
        stubStatusQuery(RebindStatus.PENDING, List.of());
        stubStatusQuery(RebindStatus.REJECTED, List.of());
        stubStatusQuery(RebindStatus.APPROVED, List.of(request));
        when(resourceRepository.findByRelease_IdAndHttpMethodAndPath(RELEASE_ID, "GET", "/users"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(1L, "admin", "同意"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("换绑资源不存在");
    }

    // ------------------------------------------------------------------
    // approve：ACTIVE 冲突仲裁
    // ------------------------------------------------------------------

    @Test
    @DisplayName("approve：其他 ACTIVE Release 在同一路径绑定了不同权限时阻塞候选 Release")
    void approve_activeReleaseConflict() {
        ServiceDefinition definition = definition(5L);
        ServiceRelease candidate = release(RELEASE_ID, definition, "2.0.0", ReleaseStatus.PENDING_APPROVAL, "rv2");
        ServiceRelease running = release(11L, definition, "1.0.0", ReleaseStatus.ACTIVE, "rv1");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING);
        ApiResource candidateResource = resource(100L, candidate, "GET", "/users", "P1", ResourceStatus.ACTIVE);
        ApiResource runningResource = resource(101L, running, "GET", "/users", "P1", ResourceStatus.ACTIVE);

        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(candidate));
        stubStatusQuery(RebindStatus.PENDING, List.of());
        stubStatusQuery(RebindStatus.REJECTED, List.of());
        stubStatusQuery(RebindStatus.APPROVED, List.of(request));
        when(resourceRepository.findByRelease_IdAndHttpMethodAndPath(RELEASE_ID, "GET", "/users"))
                .thenReturn(Optional.of(candidateResource));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID))
                .thenReturn(List.of(candidate, running));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection()))
                .thenReturn(List.of(runningResource));

        RebindApprovalService.RebindDecision decision = service.approve(1L, "admin", "同意");

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.ACTIVE_RELEASE_CONFLICT);
        assertThat(decision.pendingRebindId()).isEqualTo("1");
        assertThat(decision.message()).contains("GET /users", "运行中=P1", "候选=P2");
        assertThat(decision.resourceRevision()).isEqualTo(5L);
        assertThat(candidate.getStatus()).isEqualTo(ReleaseStatus.BLOCKED);
        assertThat(candidate.getActivationBlockedReason()).contains("运行中=P1");
        // 冲突时不得生效：不退役旧版本、不推进 revision、不发事件。
        assertThat(definition.getResourceRevision()).isEqualTo(5L);
        verify(authzEventPublisher, never()).resourceChanged(any(), any(), any(), anyLong());
        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("approve：其他 ACTIVE Release 绑定相同权限且存在空权限资源时不冲突")
    void approve_noConflictWhenBindingMatches() {
        ServiceDefinition definition = definition(8L);
        ServiceRelease candidate = release(RELEASE_ID, definition, "2.0.0", ReleaseStatus.PENDING_APPROVAL, "rv2");
        ServiceRelease running = release(11L, definition, "1.0.0", ReleaseStatus.ACTIVE, "rv1");
        ServiceRelease nullStatus = release(12L, definition, "1.0.0", null, "rv0");
        ServiceRelease retired = release(13L, definition, "1.0.0", ReleaseStatus.RETIRED, "rv0");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING);
        ApiResource candidateResource = resource(100L, candidate, "GET", "/users", "P1", ResourceStatus.ACTIVE);

        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(candidate));
        stubStatusQuery(RebindStatus.PENDING, List.of());
        stubStatusQuery(RebindStatus.REJECTED, List.of());
        stubStatusQuery(RebindStatus.APPROVED, List.of(request));
        when(resourceRepository.findByRelease_IdAndHttpMethodAndPath(RELEASE_ID, "GET", "/users"))
                .thenReturn(Optional.of(candidateResource));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID))
                .thenReturn(List.of(candidate, running, nullStatus, retired));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection()))
                .thenReturn(List.of(
                        resource(101L, running, "GET", "/users", "P2", ResourceStatus.ACTIVE),
                        resource(102L, running, "GET", "/null-perm", null, ResourceStatus.ACTIVE),
                        resource(103L, running, "GET", "/blank-perm", "  ", ResourceStatus.ACTIVE)));
        when(permissionRepository.findByCode(any())).thenReturn(Optional.empty());
        when(resourceRepository.findPermissionReferences(any(), anyCollection())).thenReturn(List.of());

        RebindApprovalService.RebindDecision decision = service.approve(1L, "admin", "同意");

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
        assertThat(decision.message()).isEqualTo("换绑已批准");
        assertThat(candidate.getStatus()).isEqualTo(ReleaseStatus.REGISTERED);
        assertThat(running.getStatus()).isEqualTo(ReleaseStatus.ACTIVE);
        assertThat(candidateResource.getPermissionCode()).isEqualTo("P2");
    }

    // ------------------------------------------------------------------
    // approve：成功生效（退役同版本、权限重算、事件）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("approve：审批通过后资源生效、同版本旧 Release 退役、权限重算并推进 revision")
    void approve_successRetiresSameVersionAndReconcilesPermissions() {
        ServiceDefinition definition = definition(5L);
        ServiceRelease candidate = release(RELEASE_ID, definition, "2.0.0", ReleaseStatus.PENDING_APPROVAL, "rv2");
        ServiceRelease sameVersion = release(11L, definition, "2.0.0", ReleaseStatus.ACTIVE, "rv1");
        ServiceRelease otherVersion = release(12L, definition, "1.0.0", ReleaseStatus.ACTIVE, "rv0");
        ServiceRelease nullStatus = release(13L, definition, "2.0.0", null, "rv0");
        ServiceRelease retired = release(14L, definition, "2.0.0", ReleaseStatus.RETIRED, "rv0");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING);
        ApiResource candidateResource = resource(100L, candidate, "GET", "/users", "P1",
                ResourceStatus.PENDING_REBIND);
        PermissionDefinition existingPermission = permission("P1", PermissionStatus.ACTIVE);

        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(candidate));
        stubStatusQuery(RebindStatus.PENDING, List.of());
        stubStatusQuery(RebindStatus.REJECTED, List.of());
        stubStatusQuery(RebindStatus.APPROVED, List.of(request));
        when(resourceRepository.findByRelease_IdAndHttpMethodAndPath(RELEASE_ID, "GET", "/users"))
                .thenReturn(Optional.of(candidateResource));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID))
                .thenReturn(List.of(candidate, sameVersion, otherVersion, nullStatus, retired));
        when(resourceRepository.findByRelease_IdInAndStatusIn(anyCollection(), anyCollection()))
                .thenReturn(List.of());
        when(permissionRepository.findByCode("P1")).thenReturn(Optional.of(existingPermission));
        when(permissionRepository.findByCode("P2")).thenReturn(Optional.empty());
        when(resourceRepository.findPermissionReferences(eq("P1"), anyCollection()))
                .thenReturn(List.of(resource(200L, candidate, "GET", "/users", "P1", ResourceStatus.ACTIVE)));
        when(resourceRepository.findPermissionReferences(eq("P2"), anyCollection()))
                .thenReturn(List.of(resource(201L, candidate, "GET", "/other", "P2", null)));

        RebindApprovalService.RebindDecision decision = service.approve(1L, "admin", "同意");

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
        assertThat(decision.releaseId()).isEqualTo("10");
        assertThat(decision.resourceRevision()).isEqualTo(6L);
        assertThat(decision.pendingRebindId()).isNull();
        assertThat(candidate.getStatus()).isEqualTo(ReleaseStatus.REGISTERED);
        assertThat(candidate.getActivationBlockedReason()).isNull();

        assertThat(candidateResource.getPermissionCode()).isEqualTo("P2");
        assertThat(candidateResource.getPendingPermissionCode()).isNull();
        assertThat(candidateResource.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
        assertThat(candidateResource.getStatusChangedAt()).isNotNull();

        // 同 serviceVersion 的旧 Release 被退役；其他版本 / 空状态 / 已退役不受影响。
        assertThat(sameVersion.getStatus()).isEqualTo(ReleaseStatus.RETIRED);
        assertThat(sameVersion.getRetiredAt()).isNotNull();
        assertThat(otherVersion.getStatus()).isEqualTo(ReleaseStatus.ACTIVE);
        assertThat(nullStatus.getStatus()).isNull();
        assertThat(retired.getStatus()).isEqualTo(ReleaseStatus.RETIRED);
        assertThat(retired.getRetiredAt()).isNull();

        // P1 仍被引用保持 ACTIVE；P2 新建后无人引用进入 ORPHANED。
        assertThat(existingPermission.getStatus()).isEqualTo(PermissionStatus.ACTIVE);
        assertThat(existingPermission.getOrphanedAt()).isNull();
        org.mockito.ArgumentCaptor<PermissionDefinition> created =
                org.mockito.ArgumentCaptor.forClass(PermissionDefinition.class);
        // 新建（ACTIVE）+ 判定为孤立后的状态更新，共两次保存同一个对象。
        verify(permissionRepository, org.mockito.Mockito.times(2)).save(created.capture());
        assertThat(created.getAllValues().getFirst().getCode()).isEqualTo("P2");
        assertThat(created.getAllValues().getFirst().getSourceServiceId()).isEqualTo(SERVICE_ID);
        assertThat(created.getAllValues().getFirst().getStatus()).isEqualTo(PermissionStatus.ORPHANED);
        assertThat(created.getAllValues().getFirst().getOrphanedAt()).isNotNull();

        assertThat(definition.getResourceRevision()).isEqualTo(6L);
        verify(serviceRepository).save(definition);
        verify(authzEventPublisher).resourceChanged(SERVICE_ID, "rv2", "2.0.0", 6L);
    }

    @Test
    @DisplayName("approve：孤立权限重新被引用时恢复 ACTIVE；禁用权限不被自动改变")
    void approve_reconcilesOrphanedAndDisabledPermissions() {
        ServiceDefinition definition = definition(1L);
        ServiceRelease candidate = release(RELEASE_ID, definition, "2.0.0", ReleaseStatus.PENDING_APPROVAL, "rv2");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING);
        ApiResource candidateResource = resource(100L, candidate, "GET", "/users", "P1",
                ResourceStatus.REBIND_REJECTED);
        PermissionDefinition orphaned = permission("P1", PermissionStatus.ORPHANED);
        PermissionDefinition disabled = permission("P2", PermissionStatus.DISABLED);

        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(candidate));
        stubStatusQuery(RebindStatus.PENDING, List.of());
        stubStatusQuery(RebindStatus.REJECTED, List.of());
        stubStatusQuery(RebindStatus.APPROVED, List.of(request));
        when(resourceRepository.findByRelease_IdAndHttpMethodAndPath(RELEASE_ID, "GET", "/users"))
                .thenReturn(Optional.of(candidateResource));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(candidate));
        when(permissionRepository.findByCode("P1")).thenReturn(Optional.of(orphaned));
        when(permissionRepository.findByCode("P2")).thenReturn(Optional.of(disabled));
        when(resourceRepository.findPermissionReferences(eq("P1"), anyCollection()))
                .thenReturn(List.of(resource(200L, candidate, "GET", "/users", "P1", ResourceStatus.ACTIVE)));
        when(resourceRepository.findPermissionReferences(eq("P2"), anyCollection()))
                .thenReturn(List.of(resource(201L, candidate, "GET", "/users", "P2", ResourceStatus.DEPRECATED)));

        RebindApprovalService.RebindDecision decision = service.approve(1L, "admin", "同意");

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
        // 回滚自动恢复（ORPHANED -> ACTIVE），且清理 orphanedAt 之外的字段不变。
        assertThat(orphaned.getStatus()).isEqualTo(PermissionStatus.ACTIVE);
        // DISABLED 是管理员决策，自动链路不改变。
        assertThat(disabled.getStatus()).isEqualTo(PermissionStatus.DISABLED);
        assertThat(disabled.getOrphanedAt()).isNull();
        // REBIND_REJECTED 资源也恢复 ACTIVE。
        assertThat(candidateResource.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
    }

    @Test
    @DisplayName("approve：空值、空白与 ANONYMOUS 权限码不触发权限重算")
    void approve_skipsNullBlankAndAnonymousPermissionCodes() {
        ServiceDefinition definition = definition(2L);
        ServiceRelease candidate = release(RELEASE_ID, definition, "2.0.0", ReleaseStatus.PENDING_APPROVAL, "rv2");
        RebindRequest anonymous = rebind(1L, "GET", "/a", null, "ANONYMOUS", RebindStatus.PENDING);
        RebindRequest blank = rebind(2L, "GET", "/b", "  ", "ANONYMOUS", RebindStatus.PENDING);
        ApiResource resourceA = resource(100L, candidate, "GET", "/a", "P1", ResourceStatus.REBIND_REJECTED);
        ApiResource resourceB = resource(101L, candidate, "GET", "/b", "P1", ResourceStatus.ACTIVE);

        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(anonymous));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(candidate));
        stubStatusQuery(RebindStatus.PENDING, List.of());
        stubStatusQuery(RebindStatus.REJECTED, List.of());
        stubStatusQuery(RebindStatus.APPROVED, List.of(anonymous, blank));
        when(resourceRepository.findByRelease_IdAndHttpMethodAndPath(RELEASE_ID, "GET", "/a"))
                .thenReturn(Optional.of(resourceA));
        when(resourceRepository.findByRelease_IdAndHttpMethodAndPath(RELEASE_ID, "GET", "/b"))
                .thenReturn(Optional.of(resourceB));
        when(releaseRepository.findByService_ServiceId(SERVICE_ID)).thenReturn(List.of(candidate));

        RebindApprovalService.RebindDecision decision = service.approve(1L, "admin", "同意");

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.ACCEPTED);
        assertThat(resourceA.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
        assertThat(resourceB.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
        verify(permissionRepository, never()).findByCode(any());
        verify(serviceRepository).save(definition);
    }

    // ------------------------------------------------------------------
    // reject
    // ------------------------------------------------------------------

    @Test
    @DisplayName("reject：待办不存在时抛出业务异常")
    void reject_requestMissing() {
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject(1L, "admin", "no"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("换绑待办不存在");
        verifyNoInteractions(releaseRepository);
    }

    @Test
    @DisplayName("reject：候选 Release 不存在时抛出业务异常")
    void reject_releaseMissing() {
        when(rebindRequestRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING)));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject(1L, "admin", "no"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("候选 Release 不存在");
    }

    @Test
    @DisplayName("reject：已拒绝的待办重放返回幂等命中，不写库")
    void reject_alreadyRejected() {
        ServiceDefinition definition = definition(3L);
        ServiceRelease release = release(RELEASE_ID, definition, "1.0.0", ReleaseStatus.BLOCKED, "rv1");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.REJECTED);
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));

        RebindApprovalService.RebindDecision decision = service.reject(1L, "admin", "no");

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED);
        assertThat(decision.pendingRebindId()).isEqualTo("1");
        assertThat(decision.resourceRevision()).isEqualTo(3L);
        assertThat(decision.message()).contains("幂等");
        verify(rebindRequestRepository, never()).save(any());
        verify(releaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("reject：正常拒绝写入原因并阻塞候选 Release")
    void reject_blocksRelease() {
        ServiceDefinition definition = definition(3L);
        ServiceRelease release = release(RELEASE_ID, definition, "1.0.0", ReleaseStatus.PENDING_APPROVAL, "rv1");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING);
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));

        RebindApprovalService.RebindDecision decision = service.reject(1L, "auditor", "不合规");

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED);
        assertThat(decision.pendingRebindId()).isEqualTo("1");
        assertThat(request.getStatus()).isEqualTo(RebindStatus.REJECTED);
        assertThat(request.getDecidedBy()).isEqualTo("auditor");
        assertThat(request.getDecisionReason()).isEqualTo("不合规");
        assertThat(request.getDecidedAt()).isNotNull();
        assertThat(release.getStatus()).isEqualTo(ReleaseStatus.BLOCKED);
        assertThat(release.getActivationBlockedReason()).isEqualTo("换绑被拒绝：不合规");
        verify(rebindRequestRepository).save(request);
        verify(releaseRepository).save(release);
    }

    @Test
    @DisplayName("reject：未填写原因时使用默认文案")
    void reject_withoutReason() {
        ServiceDefinition definition = definition(3L);
        ServiceRelease release = release(RELEASE_ID, definition, "1.0.0", ReleaseStatus.PENDING_APPROVAL, "rv1");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING);
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));

        RebindApprovalService.RebindDecision decision = service.reject(1L, "auditor", null);

        assertThat(decision.resultCode()).isEqualTo(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED);
        assertThat(release.getActivationBlockedReason()).isEqualTo("换绑被拒绝：未填写原因");
        assertThat(request.getDecisionReason()).isNull();
        assertThat(request.getDecidedAt()).isNotNull();
    }

    @Test
    @DisplayName("reject：拒绝不推进 resourceRevision、不发事件")
    void reject_doesNotAdvanceRevision() {
        ServiceDefinition definition = definition(9L);
        ServiceRelease release = release(RELEASE_ID, definition, "1.0.0", ReleaseStatus.PENDING_APPROVAL, "rv1");
        RebindRequest request = rebind(1L, "GET", "/users", "P1", "P2", RebindStatus.PENDING);
        when(rebindRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));

        service.reject(1L, "auditor", "no");

        assertThat(definition.getResourceRevision()).isEqualTo(9L);
        verify(serviceRepository, never()).save(any());
        verify(authzEventPublisher, never()).resourceChanged(any(), any(), any(), anyLong());
        assertThat(request.getDecidedAt()).isNotNull();
    }
}
