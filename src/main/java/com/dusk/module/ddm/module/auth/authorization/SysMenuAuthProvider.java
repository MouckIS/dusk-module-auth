package com.dusk.module.ddm.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2021-01-20 9:49
 */
@Component
public class SysMenuAuthProvider extends AuthorizationProvider {
    public static final String PAGES_SYS_MENU = "Pages.Sys.Menu";
    public static final String PAGES_SYS_MENU_MANAGER = "Pages.Sys.Menu.Manager";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        var sysMenu = administration.createChildPermission(PAGES_SYS_MENU, "系统菜单" , MultiTenancySides.All);
        sysMenu.createChildPermission(PAGES_SYS_MENU_MANAGER, "管理菜单" , MultiTenancySides.All);
    }
}
