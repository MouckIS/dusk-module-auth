package com.dusk.module.auth.registry.lifecycle;

import com.dusk.module.auth.registry.enums.PermissionStatus;

/**
 * Permission 生效性评估输入，见《权限优化方案-整理版》4.8。
 *
 * @param permissionCode      权限码
 * @param currentStatus       当前状态
 * @param usedByActiveRelease 是否仍被「ACTIVE Release 下的有效 Resource」引用（4.8 的判定条件）
 */
public record PermissionEvaluationInput(
        String permissionCode,
        PermissionStatus currentStatus,
        boolean usedByActiveRelease) {

    public PermissionEvaluationInput {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("PermissionEvaluationInput.permissionCode 不能为空");
        }
        permissionCode = permissionCode.trim();
        if (currentStatus == null) {
            throw new IllegalArgumentException("PermissionEvaluationInput.currentStatus 不能为空，权限：" + permissionCode);
        }
    }
}
