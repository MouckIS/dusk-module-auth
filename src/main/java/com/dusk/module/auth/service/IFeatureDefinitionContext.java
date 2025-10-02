package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.ddm.dto.ui.InputType;

import java.io.Serializable;
import java.util.List;

/**
 * @author duanxiaokang
 * @version 0.0.1
 * @date 2020/5/9 10:00
 */
public interface IFeatureDefinitionContext extends Serializable {
    TenantFeature create(String name, String defaultValue, String displayName);

    TenantFeature create(String name, String defaultValue, String displayName, String description, InputType inputType);

    TenantFeature createChildren(String name, String defaultValue, String displayName);

    TenantFeature createChildren(String name, String defaultValue, String displayName, String description, InputType inputType);

    TenantFeature createChildren(TenantFeature parent, String name, String defaultValue, String displayName);

    TenantFeature createChildren(TenantFeature parent, String name, String defaultValue, String displayName, String description, InputType inputType);

    List<TenantFeature> getFeatures();
}
