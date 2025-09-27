package com.dusk.module.ddm.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * 系统常量管理接口访问权限配置
 */
@Component
public class SysCodeAuthProvider extends AuthorizationProvider {
    public static final String PAGES_SYS_CODE = "Pages.SysCode";

    public static final String PAGES_SYS_CODE_VALUE_SAVE = "Pages.SysCode.Save";
    public static final String PAGES_SYS_CODE_VALUE_DELETE = "Pages.SysCode.Delete";


    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        Permission sysCode = administration.createChildPermission(PAGES_SYS_CODE, "常量配置", MultiTenancySides.All);
        sysCode.createChildPermission(PAGES_SYS_CODE_VALUE_SAVE, "新增/编辑常量", MultiTenancySides.All);
        sysCode.createChildPermission(PAGES_SYS_CODE_VALUE_DELETE, "删除常量", MultiTenancySides.All);
    }
}
