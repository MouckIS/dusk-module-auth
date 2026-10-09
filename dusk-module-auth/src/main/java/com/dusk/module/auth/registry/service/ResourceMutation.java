package com.dusk.module.auth.registry.service;

import com.dusk.module.auth.registry.diff.ResourceBinding;
import com.dusk.module.auth.registry.diff.ResourceKey;
import com.dusk.module.auth.registry.enums.ResourceDiffType;
import com.dusk.module.auth.registry.enums.ResourceStatus;

/**
 * 一次注册要施加到某个 Resource 上的变更意图，见《权限优化方案-整理版》4.1 / 4.9。
 *
 * <p>本类型是 {@link RegistryApplyPlanner} 的纯数据输出：只描述「目标状态」，
 * 不持有 JPA 实体、不知道持久化细节，因此可以在没有 Spring 上下文与数据库的情况下穷尽单测。</p>
 *
 * <p>字段语义（三个「码位」不能混）：</p>
 * <ul>
 *   <li>{@link #permissionCode}：<b>当前生效绑定</b>，运行时授权实际使用它；</li>
 *   <li>{@link #pendingPermissionCode}：换绑候选，仅在 {@link ResourceStatus#PENDING_REBIND} 下非空（4.9）；</li>
 *   <li>{@link #targetStatus}：本次要迁移到的资源状态。</li>
 * </ul>
 *
 * @param type                  变更类型（ADD/UPDATE/REBIND/REMOVE），仅用于日志与统计
 * @param method                HTTP 方法，已归一化为大写
 * @param path                  服务内路径
 * @param resourceId            SDK 生成的资源标识，可为空（为空时调用方保持原值）
 * @param permissionCode        目标「当前生效绑定」
 * @param pendingPermissionCode 换绑候选，非换绑时为空
 * @param targetStatus          目标资源状态
 */
public record ResourceMutation(
        ResourceDiffType type,
        String method,
        String path,
        String resourceId,
        String permissionCode,
        String pendingPermissionCode,
        ResourceStatus targetStatus) {

    public ResourceMutation {
        ResourceKey key = ResourceKey.of(method, path);
        method = key.method();
        path = key.path();
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("ResourceMutation.permissionCode 不能为空，位置：" + key.asText());
        }
        permissionCode = permissionCode.trim();
        pendingPermissionCode = pendingPermissionCode == null ? null : pendingPermissionCode.trim();
        resourceId = resourceId == null ? null : resourceId.trim();
        if (targetStatus == null) {
            throw new IllegalArgumentException("ResourceMutation.targetStatus 不能为空，位置：" + key.asText());
        }
    }

    /**
     * 便捷构造：无换绑候选。
     */
    static ResourceMutation of(ResourceDiffType type, ResourceKey key, String resourceId,
                               String permissionCode, ResourceStatus targetStatus) {
        return new ResourceMutation(type, key.method(), key.path(), resourceId, permissionCode, null, targetStatus);
    }

    /**
     * 换绑构造：生效绑定保持旧值，候选写入 pending。
     */
    static ResourceMutation rebind(ResourceKey key, String resourceId,
                                   String currentPermission, String pendingPermission) {
        return new ResourceMutation(ResourceDiffType.REBIND, key.method(), key.path(), resourceId,
                currentPermission, pendingPermission, ResourceStatus.PENDING_REBIND);
    }

    public ResourceKey key() {
        return ResourceKey.of(method, path);
    }

    /**
     * 是否匿名资源（绑定为 {@link ResourceBinding#ANONYMOUS_PERMISSION}）。
     *
     * <p>匿名资源不产生 {@code PermissionDefinition}：它表达的是「无需授权」，
     * 不是一个业务权限点。</p>
     */
    public boolean anonymous() {
        return ResourceBinding.ANONYMOUS_PERMISSION.equals(permissionCode);
    }

    /**
     * 稳定文本形式，用于日志与响应体（例如 {@code GET /users}）。
     */
    public String asText() {
        return key().asText();
    }
}
