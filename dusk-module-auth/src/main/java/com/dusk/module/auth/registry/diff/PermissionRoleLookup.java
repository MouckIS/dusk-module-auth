package com.dusk.module.auth.registry.diff;

import java.util.Set;

/**
 * 查询「持有某权限码的角色」，用于生成换绑影响面清单，见《权限优化方案-整理版》4.9。
 *
 * <p>抽成函数式接口而不是直接依赖角色仓库：Diff 引擎因此保持纯函数、可单测，
 * 而落地阶段由 Auth 侧注入基于 {@code GrantPermission} / 新 {@code RolePermission} 的实现。</p>
 */
@FunctionalInterface
public interface PermissionRoleLookup {

    /**
     * @return 持有该权限码的角色 id 集合；无法解析时返回空集合而不是 {@code null}
     */
    Set<Long> roleIdsHolding(String permissionCode);
}
