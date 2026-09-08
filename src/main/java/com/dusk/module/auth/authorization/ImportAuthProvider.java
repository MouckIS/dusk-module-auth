package com.dusk.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @CreateTime 2023/11/8
 */
@Component
public class ImportAuthProvider extends AuthorizationProvider {
    public static final String PAGES_IMPORT = "Pages.Import.Manager";
    public static final String PAGES_IMPORT_USERS = "Pages.Import.Users";
    public static final String PAGES_IMPORT_ORGANIZATION = "Pages.Import.Organization";
    public static final String PAGES_EXPORT_USERS = "Pages.Export.Users";
    public static final String PAGES_DELETE_USERS = "Pages.Delete.Users";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        Permission permission = administration.createChildPermission(PAGES_IMPORT, "账号批量导入导出", MultiTenancySides.Tenant);
        permission.createChildPermission(PAGES_IMPORT_USERS, "批量导入用户", MultiTenancySides.Tenant);
        permission.createChildPermission(PAGES_EXPORT_USERS, "批量导出用户", MultiTenancySides.Tenant);
        permission.createChildPermission(PAGES_DELETE_USERS, "批量删除用户（一次删除所有外单位或者本单位）", MultiTenancySides.Tenant);
        permission.createChildPermission(PAGES_IMPORT_ORGANIZATION, "批量导入组织", MultiTenancySides.Tenant);
    }
}
