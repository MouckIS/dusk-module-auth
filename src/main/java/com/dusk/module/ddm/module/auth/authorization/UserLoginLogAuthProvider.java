package com.dusk.module.ddm.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @description: TODO
 * @date 2022/10/28
 */
@Component
public class UserLoginLogAuthProvider extends AuthorizationProvider {
    public static final String PAGES_USER_LOGIN_LOG = "Pages.UserLoginLog";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        administration.createChildPermission(PAGES_USER_LOGIN_LOG, "查看用户登录日志");
    }
}
