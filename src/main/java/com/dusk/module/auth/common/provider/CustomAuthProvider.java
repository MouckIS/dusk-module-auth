package com.dusk.module.auth.common.provider;

import com.dusk.common.framework.auth.IAuthProvider;
import com.dusk.common.framework.auth.permission.Permission;
import com.dusk.common.framework.auth.permission.UrlPermission;
import com.dusk.common.framework.lock.annotation.Lock4j;
import com.dusk.module.auth.common.permission.IPermissionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-05-25 15:45
 */
@Component
@Primary
public class CustomAuthProvider implements IAuthProvider {

    @Autowired
    IPermissionCache permissionCache;

    @Override
    @Lock4j
    public void provideAuthInfo(String applicationName, List<String> allowAnonymousPath, Map<String, Permission> definitionPermissions, Map<String, List<UrlPermission>> urlPermissions) {
        permissionCache.addUrlPermission(applicationName, urlPermissions);
        permissionCache.addDefinitionPermission(applicationName, definitionPermissions);
        permissionCache.addAnonymousPath(applicationName, allowAnonymousPath);
    }
}
