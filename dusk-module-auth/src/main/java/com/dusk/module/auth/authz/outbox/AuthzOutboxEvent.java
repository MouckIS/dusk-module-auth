package com.dusk.module.auth.authz.outbox;

import com.dusk.common.core.entity.CreationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

/**
 * Transactional Outbox 事件行，见《权限优化方案-整理版》8.5。
 *
 * <p><b>为什么继承 {@link CreationEntity} 而不是 {@code FullAuditedEntity}</b>：Outbox 是宿主级
 * 全局数据（事件与租户无关，{@code PERMISSION_CHANGED} 的租户在载荷里），{@code FullAuditedEntity}
 * 的 {@code @Tenant} 会在 PrePersist 阶段用当前租户上下文填充 {@code tenant_id}，
 * 把全局数据错误地打上租户戳。这也是 registry 侧实体的既定取舍。</p>
 *
 * <p><b>为什么 payload 存 JSON 文本而不是关联业务表</b>：Outbox 的价值在于「与业务写入同事务提交」——
 * 存文本快照使事件内容在提交瞬间即被冻结，后续业务表的变更不会影响已排队的事件语义。</p>
 */
@Entity
@Table(name = "sys_authz_outbox_event")
@Getter
@Setter
@FieldNameConstants
public class AuthzOutboxEvent extends CreationEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型，取值与主题名同名（{@code RESOURCE_CHANGED} / {@code PERMISSION_CHANGED}）。
     */
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    /**
     * 业务幂等/检索键。
     */
    @Column(name = "biz_key", length = 255)
    private String bizKey;

    /**
     * 投递前需要刷新的读模型引用键：{@code RESOURCE_CHANGED} 存 {@code serviceId}，
     * {@code PERMISSION_CHANGED} 存 {@code tenantId:roleId}（8.5）。
     */
    @Column(name = "ref_key", length = 255)
    private String refKey;

    /**
     * 事件载荷 JSON（与线上 {@code BaseMessage.data} 同构）。
     */
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AuthzOutboxStatus status = AuthzOutboxStatus.PENDING;

    /**
     * 已尝试投递次数。
     */
    @Column(name = "attempts", nullable = false)
    private int attempts;

    /**
     * 下次允许投递的时间（退避），{@code null} 表示立即可投。
     */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * 最近一次失败原因（截断保存，仅用于排障）。
     */
    @Column(name = "last_error", length = 1000)
    private String lastError;

    /**
     * 投递成功时刻。
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
