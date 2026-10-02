package com.dusk.module.auth.service.impl;

import com.dusk.module.auth.cache.IFeatureCache;
import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.service.IFeatureRpcService;
import com.dusk.module.auth.service.IFeatureService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-07-24 15:18
 */
@DubboService
public class FeatureRpcServiceImpl implements IFeatureRpcService {
    @Resource
    private IFeatureCache featureCache;
    @Resource
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
