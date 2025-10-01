package com.dusk.module.auth.cache;

import com.dusk.module.auth.dto.TenantFeature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2021-07-26 11:40
 */
@Component
public class DefaultFeatureCache implements IFeatureCache {

    private final static Map<String, Map<String, TenantFeature>> tenantFeatureMap = new HashMap<>();

    @Override
    public synchronized void addDefaultFeature(String applicationName, List<TenantFeature> tenantFeatureList) {
        Map<String, Map<String, TenantFeature>> defaultFeature = tenantFeatureMap;
        Map<String, TenantFeature> temFeatureMap = new HashMap<>();
        for (TenantFeature feature : tenantFeatureList) {
            temFeatureMap.put(feature.getName(), feature);
        }
        defaultFeature.put(applicationName, temFeatureMap);
    }

    @Override
    public TenantFeature getDefaultFeature(String applicationName, String name) {
        Map<String, TenantFeature> temFeatureMap = tenantFeatureMap.get(applicationName);
        if (temFeatureMap != null) {
            return temFeatureMap.get(name);
        }
        return null;
    }

    @Override
    public List<TenantFeature> getDefaultFeatureList() {
        List<TenantFeature> results = new ArrayList<>();
        tenantFeatureMap.values().forEach(p -> results.addAll(p.values()));
        return results;
    }
}
