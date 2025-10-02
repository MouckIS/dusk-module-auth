package com.dusk.module.ddm.feature.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.utils.SpringContextUtils;
import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.impl.FeatureProvider;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.auth.service.IFeatureManager;
import com.dusk.module.auth.service.IFeaturePusher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通过featureprovider获取管理默认特性
 *
 * @author duanxiaokang
 * @version 0.0.1
 * @date 2020/5/9 17:05
 */

@Component
public class FeatureManager implements IFeatureManager {
    private final List<TenantFeature> tenantFeatureList;
    private final Map<String, TenantFeature> tenantFeatureMap;

    @Autowired
    private IFeatureDefinitionContext featureDefinitionContext;
    @Autowired
    private SpringContextUtils springContextUtils;
    @Autowired
    private IFeaturePusher featurePusher;


    public FeatureManager() {
        //存放初始默认特性
        this.tenantFeatureList = new ArrayList<>();
        this.tenantFeatureMap = new HashMap<>();
    }

    @PostConstruct
    //初始化特性
    public void initialize() {
        Map<String, FeatureProvider> allProviders = SpringContextUtils.getBeansOfType(FeatureProvider.class);
        for (FeatureProvider value : allProviders.values()) {
            value.setFeatures(featureDefinitionContext);
        }
        mergeFeature(featureDefinitionContext);
        //推送给auth服务
        featurePusher.provideFeatureInfo(featureDefinitionContext);
    }

    @Override
    public void mergeFeature(IFeatureDefinitionContext context) {
        this.tenantFeatureList.addAll(featureDefinitionContext.getFeatures());
        this.tenantFeatureList.forEach(tf -> {
            if (tenantFeatureMap.containsKey(tf.getName())) {
                throw new BusinessException("repeat feature name" + tf.getName());
            }
            tenantFeatureMap.put(tf.getName(), tf);
        });
    }

    @Override
    public List<TenantFeature> getTenantFeatureList() {
        return this.tenantFeatureList;
    }

    @Override
    public TenantFeature getTenantFeature(String name) {
        return tenantFeatureMap.get(name);
    }
}
