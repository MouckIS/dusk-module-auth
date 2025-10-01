package com.dusk.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @CreateTime 2022/12/16
 */
@Component
public class ExternalManagerAuthProvider extends AuthorizationProvider {

    public static final String PAGES_EXTERNAL_MANAGER = "Pages.External.Manger";

    // 外部组织管理
    public static final String PAGES_EXTERNAL_ORGANIZATION = "Pages.External.Organization";
    public static final String PAGES_EXTERNAL_ORGANIZATION_MANAGE_ORGANIZATION_TREE = "Pages.External.Organization.ManageOrganizationTree";

    // 外部单位人员管理
    public static final String PAGES_EXTERNAL_USERS = "Pages.External.Users";
    public static final String PAGES_EXTERNAL_USERS_CREATE = "Pages.External.Users.Create";
    public static final String PAGES_EXTERNAL_USERS_EDIT = "Pages.External.Users.Edit";
    public static final String PAGES_EXTERNAL_USERS_DELETE = "Pages.External.Users.Delete";
    public static final String PAGES_EXTERNAL_USERS_CHANGEPERMISSIONS = "Pages.External.Users.ChangePermissions";
    public static final String PAGES_EXTERNAL_USERS_IMPERSONATION = "Pages.External.Users.Impersonation";

    // 本单位人员管理（空菜单）
    public static final String PAGES_INNER_USERS = "Pages.Inner.Users";



    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission permission = context.createPermission(PAGES_EXTERNAL_MANAGER, "人员管控", MultiTenancySides.Tenant);
        Permission organizationUnits = permission.createChildPermission(PAGES_EXTERNAL_ORGANIZATION, "管理外单位组织机构");
        organizationUnits.createChildPermission(PAGES_EXTERNAL_ORGANIZATION_MANAGE_ORGANIZATION_TREE, "管理外单位组织机构树");

        var users = permission.createChildPermission(PAGES_EXTERNAL_USERS, "外单位用户", MultiTenancySides.Tenant);
        users.createChildPermission(PAGES_EXTERNAL_USERS_CREATE, "创建新外单位用户", MultiTenancySides.Tenant);
        users.createChildPermission(PAGES_EXTERNAL_USERS_EDIT, "编辑外单位用户", MultiTenancySides.Tenant);
        users.createChildPermission(PAGES_EXTERNAL_USERS_DELETE, "删除外单位用户", MultiTenancySides.Tenant);
        users.createChildPermission(PAGES_EXTERNAL_USERS_CHANGEPERMISSIONS, "修改权限", MultiTenancySides.Tenant);
        users.createChildPermission(PAGES_EXTERNAL_USERS_IMPERSONATION, "外单位用户登录", MultiTenancySides.Tenant);

        permission.createChildPermission(PAGES_INNER_USERS, "本单位人员管理", MultiTenancySides.Tenant);

    }
}
