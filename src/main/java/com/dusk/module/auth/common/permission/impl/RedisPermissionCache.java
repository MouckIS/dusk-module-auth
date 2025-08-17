package com.dusk.module.auth.common.permission.impl;

import com.github.dozermapper.core.Mapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.framework.auth.permission.Permission;
import com.dusk.common.framework.auth.permission.RoleInfo;
import com.dusk.common.framework.auth.permission.UrlPermission;
import com.dusk.common.framework.redis.RedisCacheCondition;
import com.dusk.common.framework.redis.RedisUtil;
import com.dusk.module.auth.common.permission.IPermissionCache;
import com.dusk.module.auth.common.permission.PermissionUtil;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.service.IGrantPermissionService;
import com.dusk.module.auth.service.ITenantPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-05-26 16:34
 */
@Conditional(RedisCacheCondition.class)
@Component
@Primary
@Slf4j
public class RedisPermissionCache implements IPermissionCache {
    private final String AUTH_PERMISSION_DEFINITION_KEY = "CRUX:AUTH:PERMISSION:DEFINITION";
    private final String AUTH_PERMISSION_URL_KEY_OLD = "CRUX:AUTH:PERMISSION:URL";
    private final String AUTH_PERMISSION_URL_KEY = "CRUX:AUTH:PERMISSION:URLS";
    private final String AUTH_PERMISSION_ANONYMOUS_KEY = "CRUX:AUTH:PERMISSION:ANONYMOUS";
    @Autowired(required = false)
    RedisUtil<Object> redisUtil;
    @Autowired
    private IGrantPermissionRepository grantPermissionRepository;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    private ITenantPermissionService tenantPermissionService;
    @Autowired
    private IGrantPermissionService grantPermissionService;
    @Autowired
    private JPAQueryFactory queryFactory;


    @Override
    public void addAnonymousPath(String applicationName, List<String> path) {
        Map<String, List<String>> allowAnonymousPath = getAllowAnonymousPath();
        allowAnonymousPath.put(applicationName, path);
        redisUtil.setCache(AUTH_PERMISSION_ANONYMOUS_KEY, allowAnonymousPath);
    }

    @Override
    public void addDefinitionPermission(String applicationName, Map<String, Permission> definition) {
        Map<String, Map<String, Permission>> definitionPermission = getDefinitionPermission();
        definitionPermission.put(applicationName, definition);
        redisUtil.setCache(AUTH_PERMISSION_DEFINITION_KEY, definitionPermission);
    }

    @Override
    public void addUrlPermission(String applicationName, Map<String, List<UrlPermission>> definition) {
        Map<String, Map<String, List<UrlPermission>>> urlPermission = getUrlPermission();
        urlPermission.put(applicationName, definition);
        redisUtil.setCache(AUTH_PERMISSION_URL_KEY, urlPermission);
        refreshAll();
    }

    @Override
    public Map<String, List<String>> getAllowAnonymousPath() {
        Object cache = redisUtil.getCache(AUTH_PERMISSION_ANONYMOUS_KEY);
        if (cache != null) {
            return (Map<String, List<String>>) cache;
        }
        return new HashMap<>();
    }

    @Override
    public Map<String, Map<String, Permission>> getDefinitionPermission() {
        Object cache = redisUtil.getCache(AUTH_PERMISSION_DEFINITION_KEY);
        if (cache != null) {
            return (Map<String, Map<String, Permission>>) cache;
        }
        return new HashMap<>();
    }

    @Override
    public Map<String, Map<String, List<UrlPermission>>> getUrlPermission() {
        //如果存在旧的权限缓存则移除
        if (redisUtil.hasKey(AUTH_PERMISSION_URL_KEY_OLD)) {
            redisUtil.deleteCache(AUTH_PERMISSION_URL_KEY_OLD);
        }
        Object cache = redisUtil.getCache(AUTH_PERMISSION_URL_KEY);
        if (cache != null) {
            return (Map<String, Map<String, List<UrlPermission>>>) cache;
        }
        return new HashMap<>();
    }


    @Override
    public void refreshAll() {
        Map<String, Map<String, List<UrlPermission>>> urlPermission = getUrlPermission();
        Map<String, List<RoleInfo>> allGrantPermission = grantPermissionService.getAll();
        Map<String, List<Long>> allTenantPermission = tenantPermissionService.getAllTenantPermission();
        for (Map<String, List<UrlPermission>> applicationPermission : urlPermission.values()) {
            PermissionUtil.setPermissionRoleInfo(allGrantPermission, applicationPermission, allTenantPermission, dozerMapper);
        }
        redisUtil.setCache(AUTH_PERMISSION_URL_KEY, urlPermission);
    }
}
