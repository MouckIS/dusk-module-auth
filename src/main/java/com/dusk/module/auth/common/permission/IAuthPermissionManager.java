package com.dusk.module.auth.common.permission;

import com.dusk.common.framework.auth.permission.Permission;
import com.dusk.common.framework.auth.permission.UrlPermission;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-05-25 9:54
 */
public interface IAuthPermissionManager {
    List<UrlPermission> getPermission(String applicationName, String url);

    Map<String, List<UrlPermission>> getApplicationPermissions(String applicationName);

    List<String> getDefinitionPermission(boolean tenantFilter);

    List<Permission> getDefinitionPermissionTree(boolean tenantFilter);

    void refreshAll();
}
