package com.dusk.module.auth.common.permission;

import com.github.dozermapper.core.Mapper;
import lombok.experimental.UtilityClass;
import com.dusk.common.core.auth.permission.RoleInfo;
import com.dusk.common.core.auth.permission.UrlPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-12-11 15:20
 */
@UtilityClass
public class PermissionUtil {

    public void setPermissionRoleInfo(Map<String, List<RoleInfo>> allGrantPermission, Map<String, List<UrlPermission>> applicationPermission, Map<String, List<Long>> allTenantPermission, Mapper dozerMapper) {
        applicationPermission.values().forEach(p -> {
            for (UrlPermission item : p) {
                List<Long> tenants = allTenantPermission.get(item.getName());
                item.setTenants(tenants);
                List<RoleInfo> roles = new ArrayList<>();

                //这里表达的是宿主的权限
                List<RoleInfo> ownerRoles = allGrantPermission.get(item.getName());
                if (ownerRoles != null) {
                    roles.addAll(ownerRoles);
                }
                //这里表达的是租户的权限，当租户未设置的时候，就算数据库里有值也忽略
                if (tenants != null) {
                    for (Long tenantId : tenants) {
                        List<RoleInfo> tenantRoles = allGrantPermission.get(tenantId + item.getName());
                        if (tenantRoles != null) {
                            roles.addAll(tenantRoles);
                        }
                    }
                }
                if (roles.size() > 0) {
                    item.setRoles(roles);
                }
            }
        });

    }

}
