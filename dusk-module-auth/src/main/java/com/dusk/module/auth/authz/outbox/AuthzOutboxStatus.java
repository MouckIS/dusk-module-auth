package com.dusk.module.auth.authz.outbox;

/**
 * Outbox 事件投递状态，见《权限优化方案-整理版》8.5。
 *
 * <p>状态机：{@code PENDING → SENT}（成功）；{@code PENDING → PENDING}（失败退避重试）；
 * {@code PENDING → FAILED}（超过最大尝试次数，告警人工介入，<b>不丢弃</b>）；
 * {@code FAILED → SENT}（人工重放）。</p>
 */
public enum AuthzOutboxStatus {

    /** 已落库，待投递。 */
    PENDING,

    /** 已成功投递到 MQ。 */
    SENT,

    /** 超过最大尝试次数，需人工介入。 */
    FAILED
}
