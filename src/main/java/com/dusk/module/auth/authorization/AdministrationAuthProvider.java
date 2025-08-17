package com.dusk.module.auth.authorization;

import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.MultiTenancySides;
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

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        context.createPermission(PAGES_ADMINISTRATION, "系统管理", MultiTenancySides.All);
    }
}
