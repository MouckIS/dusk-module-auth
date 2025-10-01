package com.dusk.module.auth.authorization;




import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

@Component
public class RoleAuthProvider extends AuthorizationProvider {

    public static final String PAGES_ROLES = "Pages.Roles";
    public static final String PAGES_ROLES_CREATEOREDIT = "Pages.Roles.CreateOrEdit";
    public static final String PAGES_ROLES_DELETE = "Pages.Roles.Delete";
    public static final String PAGES_ROLES_MANAGEPERMISSIONS = "Pages.Roles.ManagePermissions";
    public static final String PAGES_ADMINISTRATION_USERS_BIND_ROLE = "Pages.Roles.BindRole";


    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        Permission roles = administration.createChildPermission(PAGES_ROLES, "角色", MultiTenancySides.All);
        roles.createChildPermission(PAGES_ROLES_CREATEOREDIT, "创建或修改角色", MultiTenancySides.All);
        roles.createChildPermission(PAGES_ROLES_DELETE, "删除角色", MultiTenancySides.All);
        roles.createChildPermission(PAGES_ROLES_MANAGEPERMISSIONS, "编辑权限", MultiTenancySides.All);
        roles.createChildPermission(PAGES_ADMINISTRATION_USERS_BIND_ROLE, "角色绑定用户", MultiTenancySides.All);
    }

}
