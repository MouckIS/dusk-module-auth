package com.dusk.module.auth.authorization;

import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.MultiTenancySides;
import com.dusk.common.framework.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020-05-18 11:17
 */
@Component
public class CommonSettingAuthProvider extends AuthorizationProvider {
    public static final String PAGES_COMMONSETTING = "Pages.CommonSetting";
    public static final String PAGES_COMMONSETTING_CREATEOREDIT = "Pages.CommonSetting.CreateOrEdit";
    public static final String PAGES_COMMONSETTING_DELETE = "Pages.CommonSetting.Delete";


    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        Permission commonSetting = administration.createChildPermission(PAGES_COMMONSETTING, "通用配置", MultiTenancySides.Tenant);
        commonSetting.createChildPermission(PAGES_COMMONSETTING_CREATEOREDIT, "创建或更新配置信息", MultiTenancySides.Tenant);
        commonSetting.createChildPermission(PAGES_COMMONSETTING_DELETE, "删除配置信息", MultiTenancySides.Tenant);
    }
}
