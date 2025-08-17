package com.dusk.module.auth.authorization;

import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.MultiTenancySides;
import com.dusk.common.framework.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * 〈〉
 *
 * @author kefuming
 * @create 2022/2/9
 * @since 1.0.0
 */
@Component
public class PageQuickEntryAuthProvider extends AuthorizationProvider {

    public static final String PAGES_QUICK_ENTRY = "Pages.QuickEntry";
    public static final String PAGES_QUICK_ENTRY_SAVE = "Pages.QuickEntry.Save";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission quickEntryPermission = context.createPermission(PAGES_QUICK_ENTRY, "快捷入口设置项管理", MultiTenancySides.Tenant);
        quickEntryPermission.createChildPermission(PAGES_QUICK_ENTRY_SAVE,"快捷入口设置项新增",MultiTenancySides.Tenant);
    }
}
