package com.dusk.module.auth.registry.diff;

/**
 * 资源定位键：{@code (method, path)}，见《权限优化方案-整理版》4.1 与红线⑫。
 *
 * <p>Diff 的四类判定全部以该键为基准：键不存在 → ADD/REMOVE；键存在 → 再比较 permission
 * （不同为 REBIND，相同但元数据变化为 UPDATE）。</p>
 *
 * <p>归一化只做「方法转大写 + 去首尾空白」，<b>不</b>做路径前缀等业务校验——
 * 该校验属于 Snapshot 入口（{@code SnapshotValidation}），此处用于承载从数据库读出的既有数据。</p>
 *
 * @param method HTTP 方法，统一大写
 * @param path   服务内路径
 */
public record ResourceKey(String method, String path) {

    public ResourceKey {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("ResourceKey.method 不能为空");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("ResourceKey.path 不能为空");
        }
        method = method.trim().toUpperCase();
        path = path.trim();
    }

    public static ResourceKey of(String method, String path) {
        return new ResourceKey(method, path);
    }

    /**
     * 稳定的字符串形式，用于日志与告警去重。
     */
    public String asText() {
        return method + " " + path;
    }
}
