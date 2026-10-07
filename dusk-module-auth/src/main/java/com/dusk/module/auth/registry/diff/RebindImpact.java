package com.dusk.module.auth.registry.diff;

import java.util.List;

/**
 * 换绑影响面清单，见《权限优化方案-整理版》4.9：管理员必须「知情决策」，
 * 待办中要列出哪些角色将失去访问权、哪些角色将获得访问权。
 *
 * @param previousPermission 换绑前（当前生效）的权限码
 * @param newPermission      换绑后的权限码
 * @param losingRoleIds      将失去该接口访问权的角色 id
 * @param gainingRoleIds     将获得该接口访问权的角色 id
 */
public record RebindImpact(
        String previousPermission,
        String newPermission,
        List<Long> losingRoleIds,
        List<Long> gainingRoleIds) {

    public RebindImpact {
        losingRoleIds = losingRoleIds == null ? List.of() : List.copyOf(losingRoleIds);
        gainingRoleIds = gainingRoleIds == null ? List.of() : List.copyOf(gainingRoleIds);
    }

    /**
     * 无角色影响信息时的兜底（例如尚未接入角色查询）。
     */
    public static RebindImpact unknown(String previousPermission, String newPermission) {
        return new RebindImpact(previousPermission, newPermission, List.of(), List.of());
    }
}
