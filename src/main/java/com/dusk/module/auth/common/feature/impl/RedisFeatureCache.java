package com.dusk.module.auth.common.feature.impl;

import com.dusk.common.framework.feature.ui.TenantFeature;
import com.dusk.common.framework.lock.annotation.Lock4j;
import com.dusk.common.framework.redis.RedisCacheCondition;
import com.dusk.common.framework.redis.RedisUtil;
import com.dusk.module.auth.common.feature.IFeatureCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2021-07-26 10:40
 */
@Conditional(RedisCacheCondition.class)
@Component
@Primary
public class RedisFeatureCache implements IFeatureCache {
    private final String AUTH_FEATURE_MAP_KEY = "CRUX:AUTH:FEATURE";

    @Autowired
    RedisUtil<Object> redisUtil;

    @Override
    @Lock4j
    public void addDefaultFeature(String applicationName, List<TenantFeature> tenantFeatureList) {
        Map<String, Map<String, TenantFeature>> defaultFeature = getDefaultFeature();
        Map<String, TenantFeature> temFeatureMap = new HashMap<>();
        for (TenantFeature feature : tenantFeatureList) {
            temFeatureMap.put(feature.getName(), feature);
        }
        defaultFeature.put(applicationName, temFeatureMap);
        redisUtil.setCache(AUTH_FEATURE_MAP_KEY, defaultFeature);
    }

    @Override
    public TenantFeature getDefaultFeature(String applicationName, String name) {
        Map<String, Map<String, TenantFeature>> defaultFeature = getDefaultFeature();
        Map<String, TenantFeature> temFeatureMap = defaultFeature.get(applicationName);
        if (temFeatureMap != null) {
            return temFeatureMap.get(name);
        }
        return null;
    }

    @Override
    public List<TenantFeature> getDefaultFeatureList() {
        Map<String, Map<String, TenantFeature>> defaultFeature = getDefaultFeature();
        List<TenantFeature> results = new ArrayList<>();
        defaultFeature.values().forEach(p -> results.addAll(p.values()));
        return results;
    }

    private Map<String, Map<String, TenantFeature>> getDefaultFeature() {
        Object cache = redisUtil.getCache(AUTH_FEATURE_MAP_KEY);
        if (cache != null) {
            return (Map<String, Map<String, TenantFeature>>) cache;
        }
        return new HashMap<>();
    }


}
