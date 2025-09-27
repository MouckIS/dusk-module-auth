package com.dusk.module.ddm.module.auth.authorization;

import com.dusk.common.core.auth.permission.AuthorizationProvider;
import com.dusk.common.core.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2021-05-21 10:18
 */
@Component
public class FaceAuthProvider extends AuthorizationProvider {
    public static final String PAGES_ADMINISTRATION_FACES = "Pages.Administration.Faces";
    public static final String PAGES_ADMINISTRATION_FACES_ADD = "Pages.Administration.Faces.Add";
    public static final String PAGES_ADMINISTRATION_FACES_ADD_SELF = "Pages.Administration.Faces.AddSelf";

    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        var users = administration.createChildPermission(PAGES_ADMINISTRATION_FACES, "人脸", MultiTenancySides.All);
        users.createChildPermission(PAGES_ADMINISTRATION_FACES_ADD, "添加人脸", MultiTenancySides.All);
        users.createChildPermission(PAGES_ADMINISTRATION_FACES_ADD_SELF, "添加个人人脸", MultiTenancySides.All);
    }
}
