package com.dusk.module.auth.authz.event;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 角色权限变更事件载荷（Auth → Gateway），见《权限优化方案-整理版》7.2。
 *
 * <p>作用域使用 {@code HOST} / {@code TENANT:<id>} 编码（红线⑧），Host 角色事件不再因为
 * {@code tenantId == null} 被发布器跳过。{@code tenantId} 保留用于兼容旧消费端。</p>
 *
 * @param tenantScope       授权作用域，{@code HOST} 或 {@code TENANT:<id>}
 * @param tenantId          租户 ID，Host 为 {@code null}（兼容字段）
 * @param roleId            角色 ID
 * @param permissionVersion 变更后的新版本号，单调递增
 */
public record PermissionChangedEvent(@JsonProperty("tenantScope") String tenantScope,
                                     @JsonProperty("tenantId") Long tenantId,
                                     @JsonProperty("roleId") Long roleId,
                                     @JsonProperty("permissionVersion") long permissionVersion) {

    public static PermissionChangedEvent of(String tenantScope, Long tenantId, Long roleId, long permissionVersion) {
        return new PermissionChangedEvent(tenantScope, tenantId, roleId, permissionVersion);
    }

    /**
     * 幂等/检索键。
     */
    public String bizKey() {
        return "PERMISSION_CHANGED:" + tenantScope + ":" + roleId + ":" + permissionVersion;
    }

    /**
     * 读模型刷新所需的引用键：{@code scope:roleId}。
     */
    public String refKey() {
        return tenantScope + ":" + roleId;
    }

    @Override
    public String toString() {
        return "PermissionChangedEvent{tenantScope=" + tenantScope + ", roleId=" + roleId
                + ", permissionVersion=" + permissionVersion + '}';
    }
}
