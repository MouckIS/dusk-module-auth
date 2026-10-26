package com.dusk.module.auth.authz.event;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 账号状态/用户代际变更事件载荷（Auth → Gateway），见《权限优化方案-整理版》7.2。
 *
 * <p>修改 UserRole、禁用/启用用户、强制下线、修改令牌安全代际都必须推进
 * {@code subjectVersion} 并发出本事件；网关据此失效 {@code subjectCache}，
 * 避免旧 JWT 中的角色快照与禁用状态长期残留。</p>
 *
 * @param tenantScope    授权作用域，{@code HOST} 或 {@code TENANT:<id>}
 * @param userId         用户 ID
 * @param subjectVersion 变更后的用户代际版本，单调递增
 * @param enabled        账号当前是否可用
 */
public record SubjectChangedEvent(@JsonProperty("tenantScope") String tenantScope,
                                  @JsonProperty("userId") Long userId,
                                  @JsonProperty("subjectVersion") long subjectVersion,
                                  @JsonProperty("enabled") boolean enabled) {

    public static SubjectChangedEvent of(String tenantScope, Long userId, long subjectVersion, boolean enabled) {
        return new SubjectChangedEvent(tenantScope, userId, subjectVersion, enabled);
    }

    public String bizKey() {
        return "SUBJECT_CHANGED:" + tenantScope + ":" + userId + ":" + subjectVersion;
    }

    public String refKey() {
        return tenantScope + ":" + userId;
    }

    @Override
    public String toString() {
        return "SubjectChangedEvent{tenantScope=" + tenantScope + ", userId=" + userId
                + ", subjectVersion=" + subjectVersion + ", enabled=" + enabled + '}';
    }
}
