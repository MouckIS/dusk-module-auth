package com.dusk.module.auth.authz.event;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 权限变更事件载荷（Auth → Gateway），见《权限优化方案-整理版》8.4。
 *
 * <p><b>跨仓契约</b>：字段名必须与 Gateway 侧
 * {@code com.dusk.gateway.authz.event.PermissionChangedEvent} 逐字一致。两侧分属不同仓库，
 * 因此各自声明 DTO，靠 {@code @JsonProperty} 显式固定线上字段名（不依赖编译期 {@code -parameters}）。</p>
 *
 * <p><b>语义</b>：某租户下某角色的授权集合发生了变化，新版本号为 {@code permissionVersion}。
 * 网关据此<b>精确失效</b> {@code rolePermCache} 中的那一个条目。{@code permissionVersion} 是
 * <b>单调递增整数</b>，因此网关可以做真正的版本比较（仅当缓存版本更旧才失效）。</p>
 *
 * @param tenantId          租户 ID（红线⑪：角色授权按租户裁剪，缺少租户无法定位条目）
 * @param roleId            角色 ID
 * @param permissionVersion 变更后的新版本号，单调递增
 */
public record PermissionChangedEvent(@JsonProperty("tenantId") Long tenantId,
                                     @JsonProperty("roleId") Long roleId,
                                     @JsonProperty("permissionVersion") long permissionVersion) {

    public static PermissionChangedEvent of(Long tenantId, Long roleId, long permissionVersion) {
        return new PermissionChangedEvent(tenantId, roleId, permissionVersion);
    }

    /**
     * 幂等/检索键。
     */
    public String bizKey() {
        return "PERMISSION_CHANGED:" + tenantId + ":" + roleId + ":" + permissionVersion;
    }

    /**
     * 读模型刷新所需的引用键：{@code tenantId:roleId}（对应 {@code auth:authorization:{tenantId}:role:{roleId}}）。
     */
    public String refKey() {
        return tenantId + ":" + roleId;
    }

    @Override
    public String toString() {
        return "PermissionChangedEvent{tenantId=" + tenantId + ", roleId=" + roleId
                + ", permissionVersion=" + permissionVersion + '}';
    }
}
