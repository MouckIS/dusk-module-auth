package com.dusk.module.auth.registry.lifecycle;

import com.dusk.module.auth.registry.enums.PermissionStatus;
import com.dusk.module.auth.registry.enums.TransitionTrigger;

/**
 * 一条 Permission 状态迁移决定，见《权限优化方案-整理版》4.3 / 4.8。
 *
 * @param permissionCode 权限码
 * @param from           迁移前状态
 * @param to             迁移后状态
 * @param trigger        触发来源（自动链路 / 管理员手工）
 * @param reason         判定原因，用于审计与 UI 提示
 */
public record PermissionTransition(
        String permissionCode,
        PermissionStatus from,
        PermissionStatus to,
        TransitionTrigger trigger,
        String reason) {

    public boolean changed() {
        return from != to;
    }

    @Override
    public String toString() {
        if (changed()) {
            return permissionCode + ": " + from + " -> " + to + "（" + trigger + "，" + reason + "）";
        }
        return permissionCode + ": " + from + " 保持不变（" + reason + "）";
    }
}
