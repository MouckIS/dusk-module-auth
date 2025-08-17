package com.dusk.module.auth.common.metadata;

import com.dusk.common.framework.auth.permission.RoleInfo;
import com.dusk.common.framework.auth.permission.UrlPermission;
import com.dusk.common.framework.constant.AuthConstant;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-05-25 14:49
 */
@Component
public class DefaultInvocationSecurityMetadataSource {

    @Autowired
    IAuthPermissionManager authPermissionManager;

    public Collection<ConfigAttribute> getAttributes(String applicationName, String url) {
        Collection<ConfigAttribute> configAttributes = new ArrayList<>();

        Map<String, List<UrlPermission>> applicationPermissions = authPermissionManager.getApplicationPermissions(applicationName);
        if (applicationPermissions != null) {
            //首先直接用url去匹配
            List<UrlPermission> permissions = applicationPermissions.getOrDefault(url, null);
            //当直接匹配为空的时候 用antrequestmatch匹配 动态路由
            if (permissions == null) {
                AntPathMatcher antPathMatcher = new AntPathMatcher();
                for (String key : applicationPermissions.keySet()) {
                    boolean match = antPathMatcher.match(key, url);
                    if (match) {
                        //匹配到第一个就跳出循环，忽略后面的匹配
                        permissions = applicationPermissions.get(key);
                        break;
                    }
                }
            }
            if (permissions != null) {
                permissions.forEach(permission -> {
                    for (RoleInfo role : permission.getRoles()) {
                        configAttributes.add(new SecurityConfig(AuthConstant.TYPE_ROLE + role.getId()));
                    }
                    if (permission.getTenants() != null) {
                        permission.getTenants().forEach(p -> {
                            //添加租户admin标识
                            configAttributes.add(new SecurityConfig(AuthConstant.ROLE_TENANT_ADMIN + p));
                        });
                    }
                    // 宿主admin 可以访问任意接口
                    configAttributes.add(new SecurityConfig(AuthConstant.ROLE_HOST_ADMIN));
                });
            }
        }

        return configAttributes;
    }
}
