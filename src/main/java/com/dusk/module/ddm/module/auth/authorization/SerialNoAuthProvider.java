package com.dusk.module.ddm.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2021-11-24 14:37
 */
@Component
public class SerialNoAuthProvider extends AuthorizationProvider {
    public static final String PAGES_SERIAL_NO = "Pages.SerialNo";
    public static final String PAGES_SERIAL_NO_EDIT = "Pages.SerialNo.Edit";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        Permission parent = administration.createChildPermission(PAGES_SERIAL_NO, "序列号管理", MultiTenancySides.All);
        parent.createChildPermission(PAGES_SERIAL_NO_EDIT, "编辑", MultiTenancySides.All);
    }
}
