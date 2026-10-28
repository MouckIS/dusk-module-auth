package com.dusk.module.auth.registry.enums;

/**
 * Resource Diff 的结果类型，见《权限优化方案-整理版》4.1。
 *
 * <p>四种类型互斥，判定顺序为：先看 {@code (method, path)} 的存在性（{@link #ADD} / {@link #REMOVE}），
 * 再在 key 相同的前提下比较 permission（{@link #REBIND} 优先于 {@link #UPDATE}）。</p>
 */
public enum ResourceDiffType {

    /**
     * 新增：新的 {@code (method, path)} 出现。Auth 侧新建 Resource + Permission。
     */
    ADD("新增", "新的方法+路径组合出现，需要新建 Resource 与 Permission"),

    /**
     * 删除：旧的 {@code (method, path)} 消失。Resource → DEPRECATED，但 Permission 不删除。
     */
    REMOVE("删除", "旧的方法+路径组合消失，Resource 标记为废弃，Permission 保留"),

    /**
     * 修改：同 {@code (method, path)}、permission 未变，但资源元数据（如 resourceId）变化。
     */
    UPDATE("修改", "方法+路径与权限均未变，仅资源元数据变化，原地更新 Resource"),

    /**
     * 换绑：同 {@code (method, path)} 但 permission 变化。
     *
     * <p>这是本版新增的保护（4.9）：换绑会静默改变角色访问权，等效于「代码发布篡改授权决策」，
     * 因此必须挂起等待管理员确认，运行时继续按旧绑定执行。</p>
     */
    REBIND("换绑", "同一路径的权限绑定发生变化，挂起等待管理员确认，运行时按旧绑定执行");

    private final String displayName;
    private final String description;

    ResourceDiffType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
