package com.dusk.module.auth.impl;

import com.dusk.module.auth.cache.IFeatureCache;
import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.service.IFeatureRpcService;
import com.dusk.module.auth.service.IFeatureService;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-07-24 15:18
 */
@Service
public class FeatureRpcServiceImpl implements IFeatureRpcService {
    @Autowired
    private IFeatureCache featureCache;
    @Autowired
    private IFeatureService featureService;


    @Override
    public void updateFeature(String applicationName, List<TenantFeature> features) {
        featureCache.addDefaultFeature(applicationName, features);
    }

    @Override
    public String getValue(String applicationName, Long tenantId, String name) {
        TenantFeature feature = featureCache.getDefaultFeature(applicationName, name);
        String result = featureService.getFeatureValue(tenantId, name);
        return (result == null && feature != null) ? feature.getDefaultValue() : result;
    }
}
