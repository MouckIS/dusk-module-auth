package com.dusk.module.auth.registry.diff;

import com.dusk.common.core.auth.registry.ResourceVersionCalculator;

/**
 * 参与 Diff 的资源绑定：{@code (method, path) → permission}，见《权限优化方案-整理版》4.1。
 *
 * <p>匿名资源同样用本类型表达，其 {@code permission} 取
 * {@link ResourceVersionCalculator#ANONYMOUS_PERMISSION}。
 * 这样「接口从受保护改为匿名」也会被识别为 REBIND 并进入管理员确认流程，
 * 不会留下访问控制语义静默变化的通道。</p>
 *
 * @param method     HTTP 方法
 * @param path       服务内路径
 * @param permission 绑定的权限码
 * @param resourceId SDK 资源标识，可为空；仅用于判定 UPDATE（元数据变化）
 */
public record ResourceBinding(String method, String path, String permission, String resourceId) {

    /**
     * 匿名资源使用的权限码字面量，直接复用 {@code resourceVersion} 契约中的同一常量，避免两处漂移。
     */
    public static final String ANONYMOUS_PERMISSION = ResourceVersionCalculator.ANONYMOUS_PERMISSION;

    public ResourceBinding {
        ResourceKey key = new ResourceKey(method, path);
        method = key.method();
        path = key.path();
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("ResourceBinding.permission 不能为空，位置：" + key.asText());
        }
        permission = permission.trim();
        resourceId = resourceId == null ? null : resourceId.trim();
    }

    public static ResourceBinding of(String method, String path, String permission) {
        return new ResourceBinding(method, path, permission, null);
    }

    public static ResourceBinding of(String method, String path, String permission, String resourceId) {
        return new ResourceBinding(method, path, permission, resourceId);
    }

    /**
     * 匿名资源绑定。
     */
    public static ResourceBinding anonymous(String method, String path) {
        return new ResourceBinding(method, path, ANONYMOUS_PERMISSION, null);
    }

    public ResourceKey key() {
        return new ResourceKey(method, path);
    }

    public boolean isAnonymous() {
        return ANONYMOUS_PERMISSION.equals(permission);
    }
}
