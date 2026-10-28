package com.dusk.module.auth.registry.service;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.authz.ResourceRegistrationResult;
import com.dusk.module.auth.authz.service.AuthzEventPublisher;
import com.dusk.module.auth.registry.diff.ResourceKey;
import com.dusk.module.auth.registry.entity.*;
import com.dusk.module.auth.registry.enums.PermissionStatus;
import com.dusk.module.auth.registry.enums.RebindStatus;
import com.dusk.module.auth.registry.enums.ReleaseStatus;
import com.dusk.module.auth.registry.enums.ResourceStatus;
import com.dusk.module.auth.registry.lifecycle.PermissionEvaluationInput;
import com.dusk.module.auth.registry.lifecycle.PermissionLifecycleEvaluator;
import com.dusk.module.auth.registry.lifecycle.PermissionTransition;
import com.dusk.module.auth.registry.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 换绑审批闭环，见《权限优化方案-整理版》4.7。
 *
 * <p>审批通过不等于立即生效：必须确认没有其他仍可能承接流量的 Release 在同一路径上
 * 绑定不同权限，否则返回 {@code ACTIVE_RELEASE_CONFLICT}，候选 Release 保持 BLOCKED。
 * 这是「不允许新代码 + 旧权限混跑」的最后一道闸门。</p>
 */
@Service
@Slf4j
public class RebindApprovalService {

    private final IRebindRequestRepository rebindRequestRepository;
    private final IServiceReleaseRepository releaseRepository;
    private final IServiceDefinitionRepository serviceRepository;
    private final IApiResourceRepository resourceRepository;
    private final IPermissionDefinitionRepository permissionRepository;
    private final AuthzEventPublisher authzEventPublisher;

    private final PermissionLifecycleEvaluator permissionEvaluator = new PermissionLifecycleEvaluator();

    public RebindApprovalService(IRebindRequestRepository rebindRequestRepository,
                                 IServiceReleaseRepository releaseRepository,
                                 IServiceDefinitionRepository serviceRepository,
                                 IApiResourceRepository resourceRepository,
                                 IPermissionDefinitionRepository permissionRepository,
                                 AuthzEventPublisher authzEventPublisher) {
        this.rebindRequestRepository = rebindRequestRepository;
        this.releaseRepository = releaseRepository;
        this.serviceRepository = serviceRepository;
        this.resourceRepository = resourceRepository;
        this.permissionRepository = permissionRepository;
        this.authzEventPublisher = authzEventPublisher;
    }

    /**
     * 审批通过。
     *
     * @param rebindId 待办 ID
     * @param actor    审批人标识（由管理入口从可信身份取得，不能来自请求体）
     * @param reason   审批意见
     */
    @Transactional
    public RebindDecision approve(Long rebindId, String actor, String reason) {
        RebindRequest request = rebindRequestRepository.findByIdForUpdate(rebindId)
                .orElseThrow(() -> new BusinessException("换绑待办不存在：" + rebindId));
        ServiceRelease release = releaseRepository.findById(request.getCandidateReleaseId())
                .orElseThrow(() -> new BusinessException("候选 Release 不存在：" + request.getCandidateReleaseId()));
        ServiceDefinition service = release.getService();

        if (request.getStatus() == RebindStatus.APPROVED) {
            return decision(ResourceRegistrationResult.ACCEPTED, release, service.getResourceRevision(), null,
                    "换绑已批准（幂等命中）");
        }
        if (request.getStatus() == RebindStatus.REJECTED) {
            throw new BusinessException("换绑待办已被拒绝，不能直接批准；请重新发布快照生成新的待办：" + rebindId);
        }

        request.setStatus(RebindStatus.APPROVED);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(actor);
        request.setDecisionReason(reason);
        rebindRequestRepository.save(request);

        List<RebindRequest> stillPending = rebindRequestRepository.findByCandidateReleaseIdAndStatusIn(
                release.getId(), List.of(RebindStatus.PENDING));
        if (!stillPending.isEmpty()) {
            release.setStatus(ReleaseStatus.PENDING_APPROVAL);
            releaseRepository.save(release);
            RebindRequest next = stillPending.getFirst();
            log.info("换绑仍有待办，候选 Release 继续保持 pending：releaseId={}, next={}", release.getId(), next.getId());
            return decision(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED, release,
                    service.getResourceRevision(), next.getId(), "仍有换绑待办未审批");
        }

        List<RebindRequest> rejected = rebindRequestRepository.findByCandidateReleaseIdAndStatusIn(
                release.getId(), List.of(RebindStatus.REJECTED));
        if (!rejected.isEmpty()) {
            release.setStatus(ReleaseStatus.BLOCKED);
            release.setActivationBlockedReason("存在已拒绝的换绑待办");
            releaseRepository.save(release);
            return decision(ResourceRegistrationResult.ACTIVE_RELEASE_CONFLICT, release,
                    service.getResourceRevision(), rejected.getFirst().getId(), "候选 Release 存在已拒绝的换绑");
        }

        String conflict = applyApprovedBindings(release);
        if (conflict != null) {
            release.setStatus(ReleaseStatus.BLOCKED);
            release.setActivationBlockedReason(conflict);
            releaseRepository.save(release);
            log.error("换绑审批通过但仍存在 ACTIVE 冲突：releaseId={}, conflict={}", release.getId(), conflict);
            return decision(ResourceRegistrationResult.ACTIVE_RELEASE_CONFLICT, release,
                    service.getResourceRevision(), request.getId(), conflict);
        }

        release.setStatus(ReleaseStatus.REGISTERED);
        release.setActivationBlockedReason(null);
        releaseRepository.save(release);
        retireSameServiceVersion(release);

        Set<String> touched = new LinkedHashSet<>();
        for (RebindRequest approved : rebindRequestRepository.findByCandidateReleaseIdAndStatusIn(
                release.getId(), List.of(RebindStatus.APPROVED))) {
            touched.add(approved.getOldPermissionCode());
            touched.add(approved.getNewPermissionCode());
        }
        reconcilePermissions(touched, service.getServiceId(), LocalDateTime.now());

        long revision = service.nextResourceRevision();
        serviceRepository.save(service);
        authzEventPublisher.resourceChanged(service.getServiceId(), release.getResourceVersion(),
                release.getServiceVersion(), revision);

        log.info("换绑审批完成，候选 Release 已生效：serviceId={}, releaseId={}, revision={}",
                service.getServiceId(), release.getId(), revision);
        return decision(ResourceRegistrationResult.ACCEPTED, release, revision, null, "换绑已批准");
    }

    /**
     * 审批拒绝：候选 Release 保持无效，不推进 revision。
     */
    @Transactional
    public RebindDecision reject(Long rebindId, String actor, String reason) {
        RebindRequest request = rebindRequestRepository.findByIdForUpdate(rebindId)
                .orElseThrow(() -> new BusinessException("换绑待办不存在：" + rebindId));
        ServiceRelease release = releaseRepository.findById(request.getCandidateReleaseId())
                .orElseThrow(() -> new BusinessException("候选 Release 不存在：" + request.getCandidateReleaseId()));
        if (request.getStatus() == RebindStatus.REJECTED) {
            return decision(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED, release,
                    release.getService().getResourceRevision(), request.getId(), "换绑已拒绝（幂等命中）");
        }
        request.setStatus(RebindStatus.REJECTED);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(actor);
        request.setDecisionReason(reason);
        rebindRequestRepository.save(request);

        release.setStatus(ReleaseStatus.BLOCKED);
        release.setActivationBlockedReason("换绑被拒绝：" + (reason == null ? "未填写原因" : reason));
        releaseRepository.save(release);
        log.warn("换绑已被拒绝，候选 Release 阻塞：serviceId={}, releaseId={}, rebindId={}, actor={}",
                release.getService().getServiceId(), release.getId(), rebindId, actor);
        return decision(ResourceRegistrationResult.REBIND_APPROVAL_REQUIRED, release,
                release.getService().getResourceRevision(), request.getId(), "换绑已拒绝");
    }

    /**
     * 将已批准的绑定写入资源；返回 {@code null} 表示无冲突，否则返回冲突描述。
     */
    private String applyApprovedBindings(ServiceRelease release) {
        List<RebindRequest> approved = rebindRequestRepository.findByCandidateReleaseIdAndStatusIn(
                release.getId(), List.of(RebindStatus.APPROVED));
        for (RebindRequest request : approved) {
            ApiResource resource = resourceRepository
                    .findByRelease_IdAndHttpMethodAndPath(release.getId(), request.getHttpMethod(), request.getPath())
                    .orElseThrow(() -> new BusinessException("换绑资源不存在：releaseId=" + release.getId()
                            + ", " + request.getHttpMethod() + " " + request.getPath()));
            resource.setPermissionCode(request.getNewPermissionCode());
            resource.setPendingPermissionCode(null);
            if (resource.getStatus() == ResourceStatus.PENDING_REBIND || resource.getStatus() == ResourceStatus.REBIND_REJECTED) {
                resource.setStatus(ResourceStatus.ACTIVE);
                resource.setStatusChangedAt(LocalDateTime.now());
            }
            resourceRepository.save(resource);
        }
        resourceRepository.flush();
        return findActiveConflict(release, approved);
    }

    /**
     * 与所有其他仍可能承接流量的 Release 比较同一路径的绑定。
     */
    private String findActiveConflict(ServiceRelease candidate, List<RebindRequest> approved) {
        List<Long> otherReleaseIds = releaseRepository.findByService_ServiceId(candidate.getService().getServiceId())
                .stream()
                .filter(release -> !release.getId().equals(candidate.getId()))
                // 同一 serviceVersion 的旧 Release 会被本候选直接取代，不构成跨版本冲突。
                .filter(release -> !java.util.Objects.equals(release.getServiceVersion(), candidate.getServiceVersion()))
                .filter(release -> release.getStatus() != null && release.getStatus().mayServeTraffic())
                .map(ServiceRelease::getId)
                .toList();
        if (otherReleaseIds.isEmpty()) {
            return null;
        }
        Map<ResourceKey, String> active = new HashMap<>();
        for (ApiResource resource : resourceRepository.findByRelease_IdInAndStatusIn(
                otherReleaseIds, List.of(ResourceStatus.ACTIVE))) {
            if (resource.getPermissionCode() != null && !resource.getPermissionCode().isBlank()) {
                active.put(ResourceKey.of(resource.getHttpMethod(), resource.getPath()), resource.getPermissionCode());
            }
        }
        for (RebindRequest request : approved) {
            ResourceKey key = ResourceKey.of(request.getHttpMethod(), request.getPath());
            String running = active.get(key);
            if (running != null && !running.equals(request.getNewPermissionCode())) {
                return key.asText() + " 运行中=" + running + "，候选=" + request.getNewPermissionCode();
            }
        }
        return null;
    }

    /**
     * 同一 serviceVersion 的新快照取代旧 Release：同一构建版本无法通过 Nacos metadata 区分两份
     * 资源定义，保留旧身份会形成「旧快照复活」。跨 serviceVersion 的滚动发布不受影响。
     */
    private void retireSameServiceVersion(ServiceRelease candidate) {
        releaseRepository.findByService_ServiceId(candidate.getService().getServiceId()).stream()
                .filter(other -> !other.getId().equals(candidate.getId()))
                .filter(other -> java.util.Objects.equals(other.getServiceVersion(), candidate.getServiceVersion()))
                .filter(other -> other.getStatus() != null && other.getStatus().mayServeTraffic())
                .forEach(other -> {
                    other.setStatus(ReleaseStatus.RETIRED);
                    other.setRetiredAt(LocalDateTime.now());
                    releaseRepository.save(other);
                    log.info("换绑生效后退役同一发布版本的旧 Release：releaseId={}, serviceVersion={}",
                            other.getId(), other.getServiceVersion());
                });
    }

    private void reconcilePermissions(Set<String> codes, String serviceId, LocalDateTime now) {
        for (String code : codes) {
            if (code == null || code.isBlank() || "ANONYMOUS".equals(code)) {
                continue;
            }
            PermissionDefinition permission = permissionRepository.findByCode(code).orElse(null);
            if (permission == null) {
                permission = new PermissionDefinition();
                permission.setCode(code);
                permission.setSourceServiceId(serviceId);
                permission.setStatus(PermissionStatus.ACTIVE);
                permission = permissionRepository.save(permission);
            }
            PermissionTransition transition = permissionEvaluator.evaluate(new PermissionEvaluationInput(
                    code, permission.getStatus(), isReferenced(code)));
            if (transition.changed()) {
                permission.setStatus(transition.to());
                if (transition.to() == PermissionStatus.ORPHANED) {
                    permission.setOrphanedAt(now);
                }
                permissionRepository.save(permission);
                log.info("换绑审批后权限状态变更：{}", transition);
            }
        }
    }

    private boolean isReferenced(String permissionCode) {
        return resourceRepository.findPermissionReferences(permissionCode, ReleaseStatus.trafficCapableStatuses())
                .stream()
                .anyMatch(resource -> resource.getStatus() != null && resource.getStatus().effectiveForRuntime());
    }

    private RebindDecision decision(String resultCode, ServiceRelease release, long revision,
                                    Long pendingRebindId, String message) {
        return new RebindDecision(resultCode, String.valueOf(release.getId()), revision,
                pendingRebindId == null ? null : String.valueOf(pendingRebindId), message);
    }

    /**
     * 审批结果，字段与 {@link ResourceRegistrationResult} 对齐，便于 SDK 重试注册前直接判断。
     */
    public record RebindDecision(String resultCode, String releaseId, long resourceRevision,
                                 String pendingRebindId, String message) {
    }
}
