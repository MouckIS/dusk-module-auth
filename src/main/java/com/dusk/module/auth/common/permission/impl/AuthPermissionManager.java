package com.dusk.module.auth.common.permission.impl;

import lombok.Data;
import com.dusk.common.framework.auth.permission.MultiTenancySides;
import com.dusk.common.framework.auth.permission.Permission;
import com.dusk.common.framework.auth.permission.UrlPermission;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.common.permission.IPermissionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-05-25 9:44
 */
@Component
@Data
public class AuthPermissionManager implements IAuthPermissionManager {

    @Autowired
    IPermissionCache permissionCache;

    @Override
    public List<UrlPermission> getPermission(String applicationName, String url) {
        Map<String, List<UrlPermission>> permissions = permissionCache.getUrlPermission().getOrDefault(applicationName, null);
        if (permissions != null) {
            return permissions.getOrDefault(url, null);
        }
        return null;
    }

    @Override
    public Map<String, List<UrlPermission>> getApplicationPermissions(String applicationName) {
        return permissionCache.getUrlPermission().getOrDefault(applicationName, null);
    }


    @Override
    public List<String> getDefinitionPermission(boolean tenantFilter) {
        if (tenantFilter) {
            return allFlatPermission().values().stream().filter(p -> p.getMultiTenancySides().equals(MultiTenancySides.All) || p.getMultiTenancySides().equals(MultiTenancySides.Tenant))
                    .map(p -> p.getName()).collect(Collectors.toList());
        }
        return allFlatPermission().values().stream().filter(p -> p.getMultiTenancySides().equals(MultiTenancySides.Host) || p.getMultiTenancySides().equals(MultiTenancySides.All))
                .map(p -> p.getName()).collect(Collectors.toList());
    }

    @Override
    public List<Permission> getDefinitionPermissionTree(boolean tenantFilter) {
        if (tenantFilter) {
            return allFlatPermission().values().stream().filter(p -> p.getMultiTenancySides().equals(MultiTenancySides.All) || p.getMultiTenancySides().equals(MultiTenancySides.Tenant))
                    .collect(Collectors.toList());
        }
        return allFlatPermission().values().stream().filter(p -> p.getMultiTenancySides().equals(MultiTenancySides.Host) || p.getMultiTenancySides().equals(MultiTenancySides.All))
                .collect(Collectors.toList());
    }

    @Override
    public void refreshAll() {
        permissionCache.refreshAll();
    }

    private Map<String, Permission> allFlatPermission() {
        Map<String, Map<String, Permission>> definitionPermission = permissionCache.getDefinitionPermission();
        Map<String, Permission> all = new HashMap<>();
        definitionPermission.values().forEach(all::putAll);
        return all;
    }


}
