package com.dusk.module.auth.authorization;

import com.dusk.common.framework.auth.permission.AuthorizationProvider;
import com.dusk.common.framework.auth.permission.IPermissionDefinitionContext;
import com.dusk.common.framework.auth.permission.MultiTenancySides;
import com.dusk.common.framework.auth.permission.Permission;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020/11/18 11:07
 */
@Component
public class WorkerManagerAuthProvider extends AuthorizationProvider {
    public static final String PAGES_ADMINISTRATION_3D = "Pages.Administration.3D";
    public static final String PAGES_ADMINISTRATION_3D_WORKER_MANAGER = "Pages.Administration.3DWorkerManager";
    public static final String PAGES_ADMINISTRATION_3D_TAG_MANAGER= "Pages.Administration.3DTagManager";


    @Override
    public void setPermissions(IPermissionDefinitionContext context) {
        Permission administration = context.getPermissionOrNull(AdministrationAuthProvider.PAGES_ADMINISTRATION);
        Permission _3dPermission = administration.createChildPermission(PAGES_ADMINISTRATION_3D, "3D", MultiTenancySides.Tenant);
        _3dPermission.createChildPermission(PAGES_ADMINISTRATION_3D_WORKER_MANAGER, "3D作业人员管理" , MultiTenancySides.Tenant);
        _3dPermission.createChildPermission(PAGES_ADMINISTRATION_3D_TAG_MANAGER, "3D标签管理" , MultiTenancySides.Tenant);
    }
}
