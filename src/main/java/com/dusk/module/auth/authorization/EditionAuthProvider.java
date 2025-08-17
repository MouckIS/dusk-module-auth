package com.dusk.module.auth.authorization;


import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.MultiTenancySides;
import com.dusk.common.framework.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020-05-08 11:41
 */
@Component
public class EditionAuthProvider extends AuthorizationProvider {
    public static final String PAGES_EDITIONS = "Pages.Editions";
    public static final String PAGES_EDITIONS_EDIT = "Pages.Editions.Edit";
    public static final String PAGES_EDITIONS_DELETE = "Pages.Editions.Delete";
    public static final String PAGES_EDITIONS_FEATURE = "Pages.Editions.Feature";
    public static final String PAGES_EDITIONS_PERMISSION = "Pages.Editions.Permission";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        var editions = administration.createChildPermission(PAGES_EDITIONS, "版本", MultiTenancySides.Host);
        editions.createChildPermission(PAGES_EDITIONS_EDIT, "编辑版本", MultiTenancySides.Host);
        editions.createChildPermission(PAGES_EDITIONS_DELETE, "删除版本", MultiTenancySides.Host);
        editions.createChildPermission(PAGES_EDITIONS_FEATURE, "编辑特性", MultiTenancySides.Host);
        editions.createChildPermission(PAGES_EDITIONS_PERMISSION, "编辑权限", MultiTenancySides.Host);
    }
}
