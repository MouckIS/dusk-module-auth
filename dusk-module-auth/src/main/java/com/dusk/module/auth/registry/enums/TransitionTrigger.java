package com.dusk.module.auth.registry.enums;

/**
 * 状态迁移的触发来源。
 *
 * <p>区分触发来源是本方案的核心安全约束之一（见《权限优化方案-整理版》4.3 / 4.11）：
 * 由代码发布/回滚等自动化动作触发的迁移只能走 {@link #AUTO}，
 * 而「人工安全决策」（例如管理员禁用权限）只能由 {@link #MANUAL} 触发，
 * 且这类状态不得被部署动作静默回滚。</p>
 */
public enum TransitionTrigger {

    /**
     * 自动化触发：SDK 快照注册、Diff、Nacos 实例同步、回滚等。
     */
    AUTO("自动"),

    /**
     * 管理员在管理后台手工触发。
     */
    MANUAL("人工");

    private final String displayName;

    TransitionTrigger(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
