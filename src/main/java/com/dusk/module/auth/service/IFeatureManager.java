package com.dusk.module.auth.service;



import com.dusk.module.auth.dto.TenantFeature;

import java.util.List;

/**
 * @author duanxiaokang
 * @date 2020/7/20 15:20
 */
public interface IFeatureManager {

    void mergeFeature(IFeatureDefinitionContext context);

    /**
     * 返回初始化好的默认特性
     *
     * @return List
     */
    List<TenantFeature> getTenantFeatureList();

    TenantFeature getTenantFeature(String name);

}
