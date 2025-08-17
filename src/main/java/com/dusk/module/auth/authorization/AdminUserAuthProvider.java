package com.dusk.module.auth.authorization;

import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.MultiTenancySides;
import com.dusk.common.framework.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020/5/12 17:24
 */
@Component
public class AdminUserAuthProvider extends AuthorizationProvider {

    public static final String PAGES_ADMINISTRATION_USERS = "Pages.Administration.Users";
    public static final String PAGES_ADMINISTRATION_USERS_CREATE = "Pages.Administration.Users.Create";
    public static final String PAGES_ADMINISTRATION_USERS_EDIT = "Pages.Administration.Users.Edit";
    public static final String PAGES_ADMINISTRATION_USERS_DELETE = "Pages.Administration.Users.Delete";
    public static final String PAGES_ADMINISTRATION_USERS_CHANGEPERMISSIONS = "Pages.Administration.Users.ChangePermissions";
    public static final String PAGES_ADMINISTRATION_USERS_THREE_DIMENSIONAL_INTERNAL_PERSONNEL_MANAGEMENT = "Pages.Administration.Users.threeDimensionalInternalPersonnelManagement";
    public static final String PAGES_ADMINISTRATION_USERS_PRINT = "Pages.Administration.Users.Print";


    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        var users = administration.createChildPermission(PAGES_ADMINISTRATION_USERS, "用户", MultiTenancySides.All);
        users.createChildPermission(PAGES_ADMINISTRATION_USERS_CREATE, "创建新用户", MultiTenancySides.All);
        users.createChildPermission(PAGES_ADMINISTRATION_USERS_EDIT, "编辑用户", MultiTenancySides.All);
        users.createChildPermission(PAGES_ADMINISTRATION_USERS_DELETE, "删除用户", MultiTenancySides.All);
        users.createChildPermission(PAGES_ADMINISTRATION_USERS_CHANGEPERMISSIONS, "修改权限", MultiTenancySides.All);
        users.createChildPermission(PAGES_ADMINISTRATION_USERS_THREE_DIMENSIONAL_INTERNAL_PERSONNEL_MANAGEMENT, "3D内部人员管理", MultiTenancySides.All);
        users.createChildPermission(PAGES_ADMINISTRATION_USERS_PRINT, "用户信息打印", MultiTenancySides.All);

    }
}
