package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.TenantFeature;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-07-24 15:15
 */
public interface IFeatureRpcService {

    /**
     * 更新特性缓存
     *
     * @param applicationName 模块名
     * @param features        特性列表
     */
    void updateFeature(String applicationName, List<TenantFeature> features);

    /**
     * 获取特性值
     *
     * @param applicationName 模块名
     * @param tenantId        租户id
     * @param name            特性名称
     * @return 特性值
     */
    String getValue(String applicationName, Long tenantId, String name);
}
