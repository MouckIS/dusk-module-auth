package com.dusk.module.auth.authorization;


import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020-05-06 14:53
 */
@Component
public class TenantAuthProvider extends AuthorizationProvider {
    public static final String PAGES_TENANTLIST = "Pages.TenantList";
    public static final String PAGES_TENANTS = "Pages.Tenants";
    public static final String PAGES_TENANTS_CREATE = "Pages.Tenants.Create";
    public static final String PAGES_TENANTS_EDIT = "Pages.Tenants.Edit";
    public static final String PAGES_TENANTS_CHANGEFEATURES = "Pages.Tenants.ChangingFeatures";
    public static final String PAGES_TENANTS_DELETE = "Pages.Tenants.Delete";
    public static final String PAGES_TENANTS_IMPERSONATION = "Pages.Tenants.Impersonation";
    public static final String PAGES_TENANTS_PRODUCTSMANAGEMENT = "Pages.Tenants.ProductsManagement";
    public static final String PAGES_TENANTS_CHANGEPASSWORD = "Pages.Tenants.ChangePassword";


    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        Permission tenants = administration.createChildPermission(PAGES_TENANTS, "租户", MultiTenancySides.Host);
        tenants.createChildPermission(PAGES_TENANTS_CREATE, "新增租户", MultiTenancySides.Host);
        tenants.createChildPermission(PAGES_TENANTS_EDIT, "编辑租户", MultiTenancySides.Host);
        tenants.createChildPermission(PAGES_TENANTS_CHANGEFEATURES, "修改租户特性", MultiTenancySides.Host);
        tenants.createChildPermission(PAGES_TENANTS_DELETE, "删除租户", MultiTenancySides.Host);
        tenants.createChildPermission(PAGES_TENANTS_IMPERSONATION, "租户登录", MultiTenancySides.Host);
        tenants.createChildPermission(PAGES_TENANTS_PRODUCTSMANAGEMENT, "租户产品管理", MultiTenancySides.Host);
        tenants.createChildPermission(PAGES_TENANTS_CHANGEPASSWORD,"修改租户管理员密码",MultiTenancySides.Host);
    }
}
