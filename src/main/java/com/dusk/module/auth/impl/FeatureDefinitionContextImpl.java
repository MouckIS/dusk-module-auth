package com.dusk.module.auth.impl;

import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.ddm.dto.ui.InputType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author duanxiaokang
 * @version 0.0.1
 * @date 2020/5/9 10:55
 */
@Component
@Slf4j
public class FeatureDefinitionContextImpl implements IFeatureDefinitionContext {
    private static final long serialVersionUID = 7413664785667499567L;
    private final List<TenantFeature> TENANT_FEATURE_LIST;
    private String currParentName = "";

    public FeatureDefinitionContextImpl() {
        this.TENANT_FEATURE_LIST = new ArrayList<>();
    }

    @Override
    public TenantFeature create(String name, String defaultValue, String displayName) {
        return create(name, defaultValue, displayName, null, null);
    }

    @Override
    public TenantFeature create(String name, String defaultValue, String displayName, String description, InputType inputType) {
        for (TenantFeature t : this.TENANT_FEATURE_LIST) {
            if (t.getName().equals(name)) {
                this.currParentName = name;
                return t;
            }
        }
        TenantFeature tf = createFeature("", name, defaultValue, displayName, description, inputType);
        this.TENANT_FEATURE_LIST.add(tf);
        this.currParentName = name;
        return tf;
    }

    @Override
    public TenantFeature createChildren(String name, String defaultValue, String displayName) {
        return createChildren(name, defaultValue, displayName, null, null);
    }

    @Override
    public TenantFeature createChildren(String name, String defaultValue, String displayName, String description, InputType inputType) {
        for (TenantFeature t : this.TENANT_FEATURE_LIST) {
            if (t.getName().equals(name)) {
                return null;
            }
        }
        TenantFeature tf = createFeature(this.currParentName, name, defaultValue, displayName, description, inputType);
        this.TENANT_FEATURE_LIST.add(tf);
        return tf;
    }

    @Override
    public TenantFeature createChildren(TenantFeature parent, String name, String defaultValue, String displayName) {
        return createChildren(parent, name, defaultValue, displayName, "", null);
    }

    @Override
    public TenantFeature createChildren(TenantFeature parent, String name, String defaultValue, String displayName, String description, InputType inputType) {
        if (getFeatureExists(parent) == null) {
            this.TENANT_FEATURE_LIST.add(parent);
        }
        TenantFeature child = getFeatureExists(name);
        if (child == null) {
            child = createFeature(parent.getName(), name, defaultValue, displayName, description, inputType);
            this.TENANT_FEATURE_LIST.add(child);
        } else {
            child.setParentName(parent.getName());
        }
        return child;
    }

    @Override
    public List<TenantFeature> getFeatures() {
        return this.TENANT_FEATURE_LIST;
    }


    TenantFeature createFeature(String parentName, String name, String defaultValue, String displayName, String description, InputType inputType) {
        TenantFeature feature = new TenantFeature();
        feature.setParentName(parentName);
        feature.setName(name);
        feature.setDefaultValue(defaultValue);
        feature.setDisplayName(displayName);
        feature.setDescription(description);
        feature.setInputType(inputType);
        return feature;
    }

    TenantFeature getFeatureExists(TenantFeature feature) {
        return getFeatureExists(feature.getName());
    }

    TenantFeature getFeatureExists(String name) {
        for (TenantFeature tf : TENANT_FEATURE_LIST) {
            if (StringUtils.equals(name, tf.getName())) {
                return tf;
            }
        }
        return null;
    }
}
