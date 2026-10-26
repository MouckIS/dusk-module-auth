package com.dusk.module.auth.authz.outbox;

/**
 * Outbox 的投影状态，见《权限优化方案-整理版》6.2。
 *
 * <p>投影与投递是两个可恢复步骤：投影失败不得发送对应事件，也不得把 Outbox 标记为完成。
 * 因此投影状态与发送状态（{@link AuthzOutboxStatus}）必须分开记录。</p>
 */
public enum AuthzOutboxProjectionStatus {

    /**
     * 尚未投影。
     */
    PENDING,

    /**
     * 已成功投影，可以发送事件。
     */
    PROJECTED,

    /**
     * 投影失败，等待退避重试；此时禁止发送事件。
     */
    FAILED,

    /**
     * 该事件不需要投影（未知类型/兼容事件）。
     */
    SKIPPED
}
