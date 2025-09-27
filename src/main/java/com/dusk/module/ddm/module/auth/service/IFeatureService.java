package com.dusk.module.ddm.module.auth.service;


import com.dusk.common.core.feature.ui.TenantFeature;
import com.dusk.module.auth.dto.feature.FeatureValueInput;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020/5/9 9:43
 */
public interface IFeatureService {

    /**
     * 根据当前租户从数据库获取特性值列表，用于getall接口返回
     */
    Map<String, Map<String,String>>  getTenantFeatures();

    /**
     * 通过租户id获取租户的特性
     * @param tenantId 租户id
     */
    List<TenantFeature> getTenantFeaturesForEdit(Long tenantId);

    /**
     * 根据租户Id和特性树更新租户特性
     * @param tenantId 租户id
     * @param featureList 特性列表
     */
    void updateTenantFeatures(Long tenantId, List<FeatureValueInput> featureList);

    /**
     * 根据租户Id和特性值列表更新特性到数据库
     * @param tenantId 租户id
     * @param featureValues 特性列表
     */
    void updateTenantFeaturesList(Long tenantId, List<FeatureValueInput> featureValues);

    /**
     * 根据版本和特性值列表更新特性到数据库
     * @param editionId 版本id
     * @param featureList 特性列表
     */
    void setEditionFeatures(Long editionId, List<FeatureValueInput> featureList);

    /**
     * 根据版本和特性值列表更新特性到数据库
     * @param editionId 版本id
     * @param featureValues 特性列表
     */
    void setEditionFeaturesList(Long editionId, List<FeatureValueInput> featureValues);

    /**
     * 根据当前版本从数据库获取特性树
     * @param editionId 版本id
     */
    List<TenantFeature> getTenantFeaturesByEdition(Long editionId);

    /**
     * 重置租户特性
     * @param tenantId 租户id
     */
    void resetTenantSpecificFeatures(Long tenantId);

    /**
     * 返回默认租户特性列表
     */
    List<TenantFeature> getDefaultFeatures();

    /**
     * 根据名称返回当前租户特性值
     * @param tenantId 租户id
     * @param name 特性名称
     * @return String
     */
    String getFeatureValue(Long tenantId,String name);

}
