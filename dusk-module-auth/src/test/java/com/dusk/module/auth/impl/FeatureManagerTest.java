package com.dusk.module.auth.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.utils.SpringContextUtils;
import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.auth.service.IFeaturePusher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FeatureManager} 单元测试：覆盖启动时收集 Provider 特性、推送、
 * 合并去重以及 getter。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeatureManagerTest {

    @Mock
    private IFeatureDefinitionContext featureDefinitionContext;
    @Mock
    private IFeaturePusher featurePusher;
    @Mock
    private FeatureProvider providerA;
    @Mock
    private FeatureProvider providerB;

    private FeatureManager manager;

    private static TenantFeature feature(String name) {
        TenantFeature feature = new TenantFeature();
        feature.setName(name);
        return feature;
    }

    @BeforeEach
    void setUp() {
        manager = new FeatureManager();
        ReflectionTestUtils.setField(manager, "featureDefinitionContext", featureDefinitionContext);
        ReflectionTestUtils.setField(manager, "featurePusher", featurePusher);
    }

    @Test
    @DisplayName("initialize：驱动所有 Provider 注册特性并推送给缓存")
    void initializeCollectsProvidersAndPushes() {
        Map<String, FeatureProvider> providers = new LinkedHashMap<>();
        providers.put("a", providerA);
        providers.put("b", providerB);
        when(featureDefinitionContext.getFeatures()).thenReturn(List.of(feature("f1")));

        try (MockedStatic<SpringContextUtils> spring = Mockito.mockStatic(SpringContextUtils.class)) {
            spring.when(() -> SpringContextUtils.getBeansOfType(FeatureProvider.class)).thenReturn(providers);

            manager.initialize();
        }

        verify(providerA).setFeatures(featureDefinitionContext);
        verify(providerB).setFeatures(featureDefinitionContext);
        verify(featurePusher).provideFeatureInfo(featureDefinitionContext);
        assertThat(manager.getTenantFeature("f1")).isNotNull();
        assertThat(manager.getTenantFeatureList()).hasSize(1);
    }

    @Test
    @DisplayName("initialize：没有 Provider 时也能完成初始化")
    void initializeWithoutProviders() {
        when(featureDefinitionContext.getFeatures()).thenReturn(List.of());

        try (MockedStatic<SpringContextUtils> spring = Mockito.mockStatic(SpringContextUtils.class)) {
            spring.when(() -> SpringContextUtils.getBeansOfType(FeatureProvider.class)).thenReturn(Map.of());

            manager.initialize();
        }

        assertThat(manager.getTenantFeatureList()).isEmpty();
        verify(featurePusher).provideFeatureInfo(featureDefinitionContext);
    }

    @Test
    @DisplayName("mergeFeature：按名称建立索引，未知名称返回 null")
    void mergeFeatureIndexesByName() {
        when(featureDefinitionContext.getFeatures()).thenReturn(List.of(feature("f1"), feature("f2")));

        manager.mergeFeature(featureDefinitionContext);

        assertThat(manager.getTenantFeatureList()).extracting(TenantFeature::getName)
                .containsExactly("f1", "f2");
        assertThat(manager.getTenantFeature("f1")).isNotNull();
        assertThat(manager.getTenantFeature("missing")).isNull();
    }

    @Test
    @DisplayName("mergeFeature：重复特性名抛出 BusinessException")
    void mergeFeatureRejectsDuplicateNames() {
        when(featureDefinitionContext.getFeatures()).thenReturn(List.of(feature("dup")));

        manager.mergeFeature(featureDefinitionContext);

        assertThatThrownBy(() -> manager.mergeFeature(featureDefinitionContext))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("repeat feature name");
    }

    @Test
    @DisplayName("mergeFeature：实际使用的是注入字段的上下文，而不是入参上下文")
    void mergeFeatureUsesInjectedContextInsteadOfArgument() {
        IFeatureDefinitionContext argumentContext = Mockito.mock(IFeatureDefinitionContext.class);
        when(featureDefinitionContext.getFeatures()).thenReturn(List.of(feature("from-field")));
        when(argumentContext.getFeatures()).thenReturn(List.of(feature("from-argument")));

        manager.mergeFeature(argumentContext);

        assertThat(manager.getTenantFeature("from-field")).isNotNull();
        assertThat(manager.getTenantFeature("from-argument")).isNull();
    }
}
