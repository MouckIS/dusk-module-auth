package com.dusk.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2022/7/19 15:36
 */
@Component
public class CockpitAuthProvider extends AuthorizationProvider {
    
    public static final String PAGES_COCKPIT = "Pages.Cockpit";
    
    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        administration.createChildPermission(PAGES_COCKPIT, "App驾驶舱", MultiTenancySides.Tenant);
    }
}
