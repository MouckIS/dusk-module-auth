package com.dusk.module.auth.common.feature;

import com.dusk.common.framework.feature.ui.TenantFeature;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-07-26 10:39
 */
public interface IFeatureCache {
    /**
     * 推送特性定义缓存
     *
     * @param applicationName
     * @param tenantFeatureList
     */
    void addDefaultFeature(String applicationName, List<TenantFeature> tenantFeatureList);

    /**
     * 获取
     *
     * @param applicationName
     * @param name
     * @return
     */
    TenantFeature getDefaultFeature(String applicationName, String name);


    /**
     * 获取缓存列表
     *
     * @return
     */
    List<TenantFeature> getDefaultFeatureList();
}
