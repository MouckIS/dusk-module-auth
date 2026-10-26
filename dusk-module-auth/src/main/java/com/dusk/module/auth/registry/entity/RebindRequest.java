package com.dusk.module.auth.registry.entity;

import com.dusk.common.core.entity.CreationEntity;
import com.dusk.module.auth.registry.enums.RebindStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

/**
 * 换绑审批待办，见《权限优化方案-整理版》4.7 / 8.1。
 *
 * <p>换绑是<b>发布门禁</b>而不是运行时开关：候选 Release 在审批通过前不参与有效资源并集，
 * 新实例也不会进入 readiness。这样网关与业务服务始终看到同一份代码、同一套权限语义。</p>
 */
@Entity
@Table(name = "sys_registry_rebind_request",
        uniqueConstraints = @UniqueConstraint(name = "uk_registry_rebind_candidate_route",
                columnNames = {"candidate_release_id", "http_method", "path", "new_permission_code"}))
@Getter
@Setter
@FieldNameConstants
public class RebindRequest extends CreationEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 服务标识（冗余，便于按服务查询与审计）。
     */
    @Column(name = "service_id", nullable = false, length = 128)
    private String serviceId;

    /**
     * 候选 Release ID：审批通过并消除冲突后才可能进入 ACTIVE。
     */
    @Column(name = "candidate_release_id", nullable = false)
    private Long candidateReleaseId;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    @Column(name = "path", nullable = false, length = 512)
    private String path;

    /**
     * 旧绑定：审批前运行时仍按它执行。
     */
    @Column(name = "old_permission_code", nullable = false, length = 255)
    private String oldPermissionCode;

    /**
     * 新绑定：审批通过后才写入生效绑定。
     */
    @Column(name = "new_permission_code", nullable = false, length = 255)
    private String newPermissionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RebindStatus status = RebindStatus.PENDING;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /**
     * 审批人。由管理接口写入；服务间注册不得伪造该字段。
     */
    @Column(name = "decided_by", length = 128)
    private String decidedBy;

    /**
     * 审批意见/拒绝原因。
     */
    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    /**
     * 标记本次待办是否能激活候选 Release（冲突检查结果），仅用于排障。
     */
    @Column(name = "activation_blocked_reason", length = 1000)
    private String activationBlockedReason;
}
