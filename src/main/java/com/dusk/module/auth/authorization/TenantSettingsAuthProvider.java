package com.dusk.module.auth.authorization;

import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.MultiTenancySides;
import com.dusk.common.framework.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020-06-16 10:31
 */
@Component
public class TenantSettingsAuthProvider extends AuthorizationProvider {
    public static final String PAGES_ADMINISTRATION_TENANT_SETTINGS = "Pages.Administration.Tenant.Settings";
    public static final String PAGES_ADMINISTRATION_STATION_SETTINGS = "Pages.Administration.Station.Settings";


    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        administration.createChildPermission(PAGES_ADMINISTRATION_TENANT_SETTINGS, "系统设置", MultiTenancySides.Tenant);
        administration.createChildPermission(PAGES_ADMINISTRATION_STATION_SETTINGS, "厂站设置", MultiTenancySides.Tenant);
    }
}
