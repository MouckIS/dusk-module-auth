package com.dusk.module.ddm.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * Token配置
 * @author caiwenjun
 * @date 2023/5/31 9:24
 */
@Component
public class TokenConfigProvider extends AuthorizationProvider {
    public static final String PAGES_FOREVER_TOKEN_SIGN = "Pages.foreverTokenSign";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        administration.createChildPermission(PAGES_FOREVER_TOKEN_SIGN, "长久token签发");
    }
}
