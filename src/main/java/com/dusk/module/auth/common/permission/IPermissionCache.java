package com.dusk.module.auth.common.permission;

import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.permission.UrlPermission;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-05-26 16:17
 */
public interface IPermissionCache {
    void addAnonymousPath(String applicationName, List<String> path);

    void addDefinitionPermission(String applicationName, Map<String, Permission> definition);

    void addUrlPermission(String applicationName, Map<String, List<UrlPermission>> definition);

    Map<String, List<String>> getAllowAnonymousPath();

    Map<String, Map<String, Permission>> getDefinitionPermission();

    Map<String, Map<String, List<UrlPermission>>> getUrlPermission();

    void refreshAll();
}
