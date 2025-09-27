package com.dusk.module.ddm.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2022-08-31 8:43
 */
@Component
public class DynamicMenuAuthProvider extends AuthorizationProvider {
    public static final String PAGES_DYNAMIC_MENU = "Pages.DynamicMenu";
    public static final String PAGES_DYNAMIC_MENU_ADD = "Pages.DynamicMenu.Add";
    public static final String PAGES_DYNAMIC_MENU_Delete = "Pages.DynamicMenu.Delete";

    public static final String PAGES_DYNAMIC_MENU_PUBLISH = "Pages.DynamicMenu.Publish";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);

        Permission dynamicMenu = administration.createChildPermission(PAGES_DYNAMIC_MENU, "第三方菜单管理", MultiTenancySides.Tenant);
        dynamicMenu.createChildPermission(PAGES_DYNAMIC_MENU_ADD, "新增第三方菜单", MultiTenancySides.Tenant);
        dynamicMenu.createChildPermission(PAGES_DYNAMIC_MENU_Delete, "删除第三方菜单", MultiTenancySides.Tenant);
    }
}
