package com.dusk.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020-05-14 15:49
 */
@Component
public class OrganizationUnitAuthProvider extends AuthorizationProvider {
    public static final String PAGES_ADMINISTRATION_ORGANIZATIONUNITS = "Pages.Administration.OrganizationUnits";
    public static final String PAGES_ADMINISTRATION_ORGANIZATIONUNITS_MANAGEORGANIZATIONTREE = "Pages.Administration.OrganizationUnits.ManageOrganizationTree";
    public static final String PAGES_ADMINISTRATION_ORGANIZATIONUNITS_MANAGEMEMBERS = "Pages.Administration.OrganizationUnits.ManageMembers";
    public static final String PAGES_ADMINISTRATION_ORGANIZATIONUNITS_STATION_ENABLED = "Pages.Administration.OrganizationUnits.Station.Enabled";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        Permission organizationUnits = administration.createChildPermission(PAGES_ADMINISTRATION_ORGANIZATIONUNITS, "管理组织机构");
        organizationUnits.createChildPermission(PAGES_ADMINISTRATION_ORGANIZATIONUNITS_MANAGEORGANIZATIONTREE, "管理组织机构树");
        organizationUnits.createChildPermission(PAGES_ADMINISTRATION_ORGANIZATIONUNITS_MANAGEMEMBERS, "管理成员");
        organizationUnits.createChildPermission(PAGES_ADMINISTRATION_ORGANIZATIONUNITS_STATION_ENABLED, "设置厂站可用/不可用", MultiTenancySides.Tenant);
    }
}
