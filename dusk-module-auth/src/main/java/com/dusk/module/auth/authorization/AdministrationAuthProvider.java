package com.dusk.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020-05-12 17:27
 */
@Component
//权限的根设置order 最小 一般写1，剩余的权限如果想固定排序顺序的 则自己安排order
@Order(1)
public class AdministrationAuthProvider extends AuthorizationProvider {
    public static final String PAGES_ADMINISTRATION = "Pages.Administration";

    /**
     * 创建系统管理根权限。
     *
     * <p>单体化后 auth / metadata / notification 三个模块的 AuthProvider 会跑在同一个
     * {@code PermissionDefinitionContext} 里，而 {@code Pages.Administration} 这个根权限码被三处共用。
     * 原先这里无条件 {@code createPermission}，只要执行顺序稍晚就会抛
     * {@code BusinessException: There is already a permission with name: Pages.Administration}。
     * 这里改成「有则复用、无则创建」，与 metadata 的 {@code SerialNoAuthProvider}、
     * notification 的 {@code NotificationAuthProvider} 保持一致；中文名仍由本类（{@code @Order(1)}）优先写入。</p>
     */
    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission existing = context.getPermissionOrNull(PAGES_ADMINISTRATION);
        if (existing == null) {
            context.createPermission(PAGES_ADMINISTRATION, "系统管理", MultiTenancySides.All);
        }
    }
}
