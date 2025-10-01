package com.dusk.module.auth.impl;


import com.dusk.module.auth.service.IFeatureDefinitionContext;

/**
 * @author duanxiaokang
 * @version 0.0.1
 * @date 2020/5/9 9:57
 */
public abstract class FeatureProvider {
    public abstract void setFeatures(IFeatureDefinitionContext context);
}
