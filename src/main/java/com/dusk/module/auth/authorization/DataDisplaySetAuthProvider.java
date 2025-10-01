package com.dusk.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * 〈〉
 *
 * @author kefuming
 * @create 2022/2/9
 * @since 1.0.0
 */
@Component
public class DataDisplaySetAuthProvider extends AuthorizationProvider {


    public static final String PAGES_DATA_DISPLAY_SET = "Pages.DataDisplaySet";
    public static final String PAGES_DATA_DISPLAY_SET_SAVE = "Pages.DataDisplaySet.Save";


    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission dataDisplaySet = context.createPermission(PAGES_DATA_DISPLAY_SET, "数据展示设置项管理", MultiTenancySides.Tenant);
        dataDisplaySet.createChildPermission(PAGES_DATA_DISPLAY_SET_SAVE,"数据展示设置项的新增",MultiTenancySides.Tenant);

    }
}
