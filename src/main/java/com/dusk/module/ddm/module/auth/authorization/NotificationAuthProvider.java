package com.dusk.module.ddm.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author: pengmengjiang
 * @date: 2021/9/15 9:41
 */
@Component
public class NotificationAuthProvider extends AuthorizationProvider {

    public final static String PAGES_NOTIFICATION = "Pages.Administration.Notification";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission root = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        root.createChildPermission(PAGES_NOTIFICATION, "消息中心", MultiTenancySides.Tenant);
    }
}
