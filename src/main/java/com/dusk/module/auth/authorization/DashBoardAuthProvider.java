package com.dusk.module.auth.authorization;

import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.MultiTenancySides;
import com.dusk.common.framework.auth.permission.Permission;
import org.springframework.stereotype.Component;

@Component
public class DashBoardAuthProvider extends AuthorizationProvider {

    public static final String PAGES_DASHBOARD = "Pages.DashBoard";

    public static final String PAGES_DASHBOARD_THEME = "Pages.DashBoard.Theme";
    public static final String PAGES_DASHBOARD_THEME_EDIT = "Pages.DashBoard.Theme.Edit";
    public static final String PAGES_DASHBOARD_THEME_DELETE = "Pages.DashBoard.Theme.Delete";

    public static final String PAGES_DASHBOARD_THEME_MODULE = "Pages.DashBoard.Theme.Module";
    public static final String PAGES_DASHBOARD_THEME_MODULE_EDIT = "Pages.DashBoard.Theme.Module.Edit";
    public static final String PAGES_DASHBOARD_THEME_MODULE_IMPORT = "Pages.DashBoard.Theme.Module.Import";
    public static final String PAGES_DASHBOARD_THEME_MODULE_EXPORT = "Pages.DashBoard.Theme.Module.Export";
    public static final String PAGES_DASHBOARD_THEME_MODULE_DELETE = "Pages.DashBoard.Theme.Module.Delete";
    public static final String PAGES_DASHBOARD_THEME_MODULE_ROUTE = "Pages.DashBoard.Theme.Module.Route";


    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);

        Permission dashBoard = administration.createChildPermission(PAGES_DASHBOARD, "数据大屏", MultiTenancySides.Tenant);

        /**
         * 主题管理
         */
        Permission theme = dashBoard.createChildPermission(PAGES_DASHBOARD_THEME, "主题管理", MultiTenancySides.Tenant);
        theme.createChildPermission(PAGES_DASHBOARD_THEME_EDIT, "创建或修改主题", MultiTenancySides.Tenant);
        theme.createChildPermission(PAGES_DASHBOARD_THEME_DELETE, "删除主题", MultiTenancySides.Tenant);

        /**
         * 模块管理
         */
        Permission module = dashBoard.createChildPermission(PAGES_DASHBOARD_THEME_MODULE, "数据模块", MultiTenancySides.Tenant);
        module.createChildPermission(PAGES_DASHBOARD_THEME_MODULE_EDIT, "新增或修改数据模块", MultiTenancySides.Tenant);
        module.createChildPermission(PAGES_DASHBOARD_THEME_MODULE_IMPORT, "导入模块配置", MultiTenancySides.Tenant);
        module.createChildPermission(PAGES_DASHBOARD_THEME_MODULE_EXPORT, "导出模块配置", MultiTenancySides.Tenant);
        module.createChildPermission(PAGES_DASHBOARD_THEME_MODULE_DELETE, "删除数据模块", MultiTenancySides.Tenant);
        module.createChildPermission(PAGES_DASHBOARD_THEME_MODULE_ROUTE, "数据模块路由", MultiTenancySides.Tenant);
    }

}
