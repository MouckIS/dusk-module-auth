package com.dusk.module.auth.impl;

import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.metadata.dto.ui.InputType;
import com.dusk.module.metadata.dto.ui.SingerLineString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FeatureDefinitionContextImpl} 单元测试：覆盖特性创建、重复特性复用、
 * 子特性挂载（含父/子已存在时的更新）与当前父节点状态。
 */
class FeatureDefinitionContextImplTest {

    private FeatureDefinitionContextImpl context;

    @BeforeEach
    void setUp() {
        context = new FeatureDefinitionContextImpl();
    }

    @Test
    @DisplayName("create：新特性默认挂在根节点下，并记录为当前父节点")
    void createNewFeature() {
        TenantFeature feature = context.create("root", "true", "根特性");

        assertThat(feature.getParentName()).isEmpty();
        assertThat(feature.getName()).isEqualTo("root");
        assertThat(feature.getDefaultValue()).isEqualTo("true");
        assertThat(feature.getDisplayName()).isEqualTo("根特性");
        assertThat(context.getFeatures()).containsExactly(feature);
    }

    @Test
    @DisplayName("create：带描述与输入类型时完整写入")
    void createWithDescriptionAndInputType() {
        InputType inputType = new SingerLineString();

        TenantFeature feature = context.create("f", "v", "显示名", "描述", inputType);

        assertThat(feature.getDescription()).isEqualTo("描述");
        assertThat(feature.getInputType()).isSameAs(inputType);
    }

    @Test
    @DisplayName("create：同名特性直接复用并更新当前父节点")
    void createExistingFeatureReturnsSameInstance() {
        TenantFeature first = context.create("f", "v", "第一次");
        TenantFeature second = context.create("f", "other", "第二次");

        assertThat(second).isSameAs(first);
        assertThat(context.getFeatures()).hasSize(1);

        TenantFeature child = context.createChildren("child", "1", "子特性");
        assertThat(child.getParentName()).isEqualTo("f");
    }

    @Test
    @DisplayName("createChildren(name)：挂在最近一次 create 的父节点下")
    void createChildrenUsesCurrentParent() {
        context.create("parent", "1", "父特性");

        TenantFeature child = context.createChildren("child", "0", "子特性");

        assertThat(child.getParentName()).isEqualTo("parent");
    }

    @Test
    @DisplayName("createChildren(name)：同名子特性已存在时返回 null")
    void createChildrenReturnsNullWhenExists() {
        context.create("parent", "1", "父特性");
        context.createChildren("child", "0", "子特性");

        assertThat(context.createChildren("child", "0", "重复子特性")).isNull();
        assertThat(context.getFeatures()).hasSize(2);
    }

    @Test
    @DisplayName("createChildren(parent,...)：父节点不存在时先加入父节点")
    void createChildrenAddsMissingParent() {
        TenantFeature parent = new TenantFeature();
        parent.setName("parent");

        TenantFeature child = context.createChildren(parent, "child", "0", "子特性", "描述", null);

        assertThat(context.getFeatures()).containsExactly(parent, child);
        assertThat(child.getParentName()).isEqualTo("parent");
    }

    @Test
    @DisplayName("createChildren(parent,...)：父节点已存在时不重复加入")
    void createChildrenReusesExistingParent() {
        TenantFeature parent = context.create("parent", "1", "父特性");

        TenantFeature child = context.createChildren(parent, "child", "0", "子特性", "描述", null);

        assertThat(context.getFeatures()).containsExactly(parent, child);
    }

    @Test
    @DisplayName("createChildren(parent,...)：子特性已存在时只更新父节点名称")
    void createChildrenUpdatesExistingChildParent() {
        TenantFeature parentA = context.create("parentA", "1", "父A");
        TenantFeature child = context.createChildren(parentA, "child", "0", "子特性", "", null);

        TenantFeature parentB = new TenantFeature();
        parentB.setName("parentB");
        TenantFeature result = context.createChildren(parentB, "child", "0", "子特性", "", null);

        assertThat(result).isSameAs(child);
        assertThat(result.getParentName()).isEqualTo("parentB");
        assertThat(context.getFeatures()).containsExactly(parentA, child, parentB);
    }

    @Test
    @DisplayName("createChildren 四参重载使用空描述")
    void createChildrenFourArgsUsesEmptyDescription() {
        TenantFeature parent = new TenantFeature();
        parent.setName("parent");

        TenantFeature child = context.createChildren(parent, "child", "0", "子特性");

        assertThat(child.getDescription()).isEmpty();
    }

    @Test
    @DisplayName("getFeatureExists：按名称查找，未命中返回 null")
    void getFeatureExistsByName() {
        TenantFeature feature = context.create("f", "v", "特性");

        assertThat(context.getFeatureExists("f")).isSameAs(feature);
        assertThat(context.getFeatureExists("missing")).isNull();
    }

    @Test
    @DisplayName("getFeatureExists(TenantFeature)：按对象名称查找")
    void getFeatureExistsByFeature() {
        TenantFeature feature = context.create("f", "v", "特性");
        TenantFeature probe = new TenantFeature();
        probe.setName("f");

        assertThat(context.getFeatureExists(probe)).isSameAs(feature);
        assertThat(context.getFeatureExists(new TenantFeature())).isNull();
    }

    @Test
    @DisplayName("createFeature：直接构造特性对象")
    void createFeatureBuildsObject() {
        TenantFeature feature = context.createFeature("parent", "name", "def", "显示", "描述", null);

        assertThat(feature.getParentName()).isEqualTo("parent");
        assertThat(feature.getName()).isEqualTo("name");
        assertThat(feature.getDefaultValue()).isEqualTo("def");
        assertThat(feature.getDisplayName()).isEqualTo("显示");
        assertThat(feature.getDescription()).isEqualTo("描述");
    }

    @Test
    @DisplayName("getFeatures 返回内部可变列表")
    void getFeaturesReturnsInternalList() {
        context.create("a", "1", "A");
        context.create("b", "2", "B");

        List<TenantFeature> features = context.getFeatures();

        assertThat(features).extracting(TenantFeature::getName).containsExactly("a", "b");
    }
}
