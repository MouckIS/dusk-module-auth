package com.dusk.module.auth.service.impl;

import com.dusk.module.auth.cache.IFeatureCache;
import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.service.IFeatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FeatureRpcServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class FeatureRpcServiceImplTest {

    private static final String APP = "auth-app";

    @Mock
    private IFeatureCache featureCache;
    @Mock
    private IFeatureService featureService;

    private FeatureRpcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FeatureRpcServiceImpl();
        ReflectionTestUtils.setField(service, "featureCache", featureCache);
        ReflectionTestUtils.setField(service, "featureService", featureService);
    }

    private static TenantFeature feature(String defaultValue) {
        TenantFeature tenantFeature = new TenantFeature();
        tenantFeature.setDefaultValue(defaultValue);
        return tenantFeature;
    }

    @Test
    @DisplayName("updateFeature：转发到特性缓存")
    void updateFeatureDelegatesToCache() {
        List<TenantFeature> features = List.of(feature("v"));

        service.updateFeature(APP, features);

        verify(featureCache).addDefaultFeature(APP, features);
    }

    @Test
    @DisplayName("getValue：租户配置存在时直接返回配置值")
    void getValueReturnsTenantValue() {
        when(featureService.getFeatureValue(1L, "f")).thenReturn("from-tenant");

        assertThat(service.getValue(APP, 1L, "f")).isEqualTo("from-tenant");
    }

    @Test
    @DisplayName("getValue：配置为空且存在默认特性时返回默认值")
    void getValueFallsBackToDefaultValue() {
        when(featureService.getFeatureValue(1L, "f")).thenReturn(null);
        when(featureCache.getDefaultFeature(APP, "f")).thenReturn(feature("default-value"));

        assertThat(service.getValue(APP, 1L, "f")).isEqualTo("default-value");
    }

    @Test
    @DisplayName("getValue：配置为空且无默认特性时返回 null")
    void getValueReturnsNullWithoutDefaultFeature() {
        when(featureService.getFeatureValue(1L, "f")).thenReturn(null);
        when(featureCache.getDefaultFeature(APP, "f")).thenReturn(null);

        assertThat(service.getValue(APP, 1L, "f")).isNull();
    }
}
