package com.dusk.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author panyanlin
 * @date 2021/5/12 17:24
 */
@Component
public class UserFingerprintAuthProvider extends AuthorizationProvider {

    public static final String PAGES_FINGERPRINT = "Pages.Fingerprint";
    public static final String PAGES_FINGERPRINT_SAVE = "Pages.Fingerprint.Save";
    public static final String PAGES_FINGERPRINT_DELETE = "Pages.Fingerprint.Delete";
    public static final String PAGES_FINGERPRINT_SAVE_PRIVATE = "Pages.Fingerprint.Save.Private";
    public static final String PAGES_FINGERPRINT_DELETE_PRIVATE = "Pages.Fingerprint.Delete.Private";


    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission fingerprint = context.createPermission(PAGES_FINGERPRINT, "用户指纹管理", MultiTenancySides.Tenant);
        fingerprint.createChildPermission(PAGES_FINGERPRINT_SAVE, "新增/编辑他人指纹", MultiTenancySides.Tenant);
        fingerprint.createChildPermission(PAGES_FINGERPRINT_DELETE, "删除他人指纹", MultiTenancySides.Tenant);
        fingerprint.createChildPermission(PAGES_FINGERPRINT_SAVE_PRIVATE, "新增/编辑个人指纹", MultiTenancySides.Tenant);
        fingerprint.createChildPermission(PAGES_FINGERPRINT_DELETE_PRIVATE, "删除个人指纹", MultiTenancySides.Tenant);
    }
}
