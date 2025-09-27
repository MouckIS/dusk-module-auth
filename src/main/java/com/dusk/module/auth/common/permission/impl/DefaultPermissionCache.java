package com.dusk.module.auth.common.permission.impl;

import com.github.dozermapper.core.Mapper;
import lombok.Data;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.permission.RoleInfo;
import com.dusk.common.core.auth.permission.UrlPermission;
import com.dusk.module.auth.common.permission.IPermissionCache;
import com.dusk.module.auth.common.permission.PermissionUtil;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.service.IGrantPermissionService;
import com.dusk.module.auth.service.ITenantPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-05-26 16:32
 */
@Data
@Component
public class DefaultPermissionCache implements IPermissionCache {

    private final Map<String, List<String>> allowAnonymousPath;
    private final Map<String, Map<String, List<UrlPermission>>> urlPermission;
    private final Map<String, Map<String, Permission>> definitionPermission;

    @Autowired
    private IGrantPermissionRepository grantPermissionRepository;
    @Autowired
    private IGrantPermissionService grantPermissionService;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    private ITenantPermissionService tenantPermissionService;

    @Override
    public void addAnonymousPath(String applicationName, List<String> path) {
        allowAnonymousPath.put(applicationName, path);
    }

    @Override
    public void addDefinitionPermission(String applicationName, Map<String, Permission> definition) {
        definitionPermission.put(applicationName, definition);
    }

    @Override
    public void addUrlPermission(String applicationName, Map<String, List<UrlPermission>> definition) {
        urlPermission.put(applicationName, definition);
        refreshAll();
    }


    @Override
    public void refreshAll() {
        Map<String, Map<String, List<UrlPermission>>> urlPermission = getUrlPermission();
        Map<String, List<RoleInfo>> allGrantPermission = grantPermissionService.getAll();
        Map<String, List<Long>> allTenantPermission = tenantPermissionService.getAllTenantPermission();
        for (Map<String, List<UrlPermission>> applicationPermission : urlPermission.values()) {
            PermissionUtil.setPermissionRoleInfo(allGrantPermission, applicationPermission, allTenantPermission, dozerMapper);
        }
    }
}
