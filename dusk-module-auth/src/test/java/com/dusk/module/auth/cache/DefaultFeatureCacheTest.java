package com.dusk.module.auth.cache;

import com.dusk.module.auth.dto.TenantFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DefaultFeatureCache} 单元测试：覆盖按应用维度的默认特性缓存、命中/未命中
 * 与多应用打平列表。
 */
class DefaultFeatureCacheTest {

    private DefaultFeatureCache cache;

    private static TenantFeature feature(String name, String defaultValue) {
        TenantFeature feature = new TenantFeature();
        feature.setName(name);
        feature.setDefaultValue(defaultValue);
        return feature;
    }

    @BeforeEach
    void setUp() {
        cache = new DefaultFeatureCache();
        Map<String, Map<String, TenantFeature>> tenantFeatureMap =
                (Map<String, Map<String, TenantFeature>>) ReflectionTestUtils.getField(cache, "tenantFeatureMap");
        tenantFeatureMap.clear();
    }

    @Test
    @DisplayName("按应用缓存特性并支持按名称读取")
    void addAndGetFeature() {
        cache.addDefaultFeature("app", List.of(feature("f1", "v1"), feature("f2", "v2")));

        assertThat(cache.getDefaultFeature("app", "f1").getDefaultValue()).isEqualTo("v1");
        assertThat(cache.getDefaultFeature("app", "missing")).isNull();
    }

    @Test
    @DisplayName("未注册的应用返回 null")
    void getFeatureFromUnknownApplication() {
        assertThat(cache.getDefaultFeature("unknown", "f1")).isNull();
    }

    @Test
    @DisplayName("getDefaultFeatureList 汇总所有应用的特性")
    void getDefaultFeatureListMergesApplications() {
        cache.addDefaultFeature("app-a", List.of(feature("a", "1")));
        cache.addDefaultFeature("app-b", List.of(feature("b", "2")));

        assertThat(cache.getDefaultFeatureList())
                .extracting(TenantFeature::getName)
                .containsExactlyInAnyOrder("a", "b");
    }

    @Test
    @DisplayName("同一应用重复注册会替换旧特性集合")
    void addDefaultFeatureReplacesApplication() {
        cache.addDefaultFeature("app", List.of(feature("old", "1")));
        cache.addDefaultFeature("app", List.of(feature("new", "2")));

        assertThat(cache.getDefaultFeature("app", "old")).isNull();
        assertThat(cache.getDefaultFeature("app", "new")).isNotNull();
        assertThat(cache.getDefaultFeatureList()).extracting(TenantFeature::getName).containsExactly("new");
    }
}
