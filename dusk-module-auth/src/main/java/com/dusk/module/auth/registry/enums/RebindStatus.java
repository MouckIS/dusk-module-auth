package com.dusk.module.auth.registry.enums;

/**
 * 换绑待办状态，见《权限优化方案-整理版》4.7。
 */
public enum RebindStatus {

    /**
     * 待审批：候选 Release 不得接流量。
     */
    PENDING("待审批"),

    /**
     * 已批准：资源绑定可在冲突检查通过后生效。
     */
    APPROVED("已批准"),

    /**
     * 已拒绝：候选 Release 保持无效；同一快照再次注册时复用本待办。
     */
    REJECTED("已拒绝");

    private final String displayName;

    RebindStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
