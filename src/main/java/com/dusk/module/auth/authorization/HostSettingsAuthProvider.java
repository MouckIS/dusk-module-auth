package com.dusk.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.module.auth.setting.config.MultiTenancyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020-05-21 16:49
 */
@Component
public class HostSettingsAuthProvider extends AuthorizationProvider {
    public static final String PAGES_ADMINISTRATION_HOST_SETTINGS = "Pages.Administration.Host.Settings";
    public static final String PAGES_ADMINISTRATION_HOST_MAINTENANCE = "Pages.Administration.Host.Maintenance";
    public static final String PAGES_ADMINISTRATION_HOST_DASHBOARD = "Pages.Administration.Host.Dashboard";

    @Autowired
    private MultiTenancyConfig multiTenancyConfig;

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        administration.createChildPermission(PAGES_ADMINISTRATION_HOST_SETTINGS, "设置", MultiTenancySides.Host);
        administration.createChildPermission(PAGES_ADMINISTRATION_HOST_MAINTENANCE, "维护系统设置", multiTenancyConfig.isEnable() ? MultiTenancySides.Host : MultiTenancySides.Tenant);
        administration.createChildPermission(PAGES_ADMINISTRATION_HOST_DASHBOARD, "工作台", MultiTenancySides.Host);
    }
}
