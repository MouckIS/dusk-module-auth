package com.dusk.module.auth.authorization;

import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.MultiTenancySides;
import com.dusk.common.framework.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @Author: pengmengjiang
 * @Date: 2021/9/15 9:41
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
