package com.dusk.module.auth.authorization;

import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2022/9/22 8:34
 */
@Component
public class StationAuthProvider extends AuthorizationProvider {
    public static final String PAGES_ADMINISTRATION_STATION = "Pages.Administration.Station";
    public static final String PAGES_ADMINISTRATION_STATION_MANAGE_STATION = "Pages.Administration.Station.ManageStation";
    public static final String PAGES_ADMINISTRATION_STATION_MANAGE_MEMBERS = "Pages.Administration.Station.ManageMembers";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        Permission stations = administration.createChildPermission(PAGES_ADMINISTRATION_STATION, "厂站管理");
        stations.createChildPermission(PAGES_ADMINISTRATION_STATION_MANAGE_STATION, "厂站维护");
        stations.createChildPermission(PAGES_ADMINISTRATION_STATION_MANAGE_MEMBERS, "厂站成员维护");
    }
}
