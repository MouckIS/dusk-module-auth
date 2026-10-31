package com.dusk.module.auth.impl;

import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.service.IFeatureRpcService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link FeatureChecker} 单元测试：覆盖无租户上下文、按当前租户取值以及布尔开关解析。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeatureCheckerTest {

    private static final String APP = "auth-app";

    @Mock
    private IFeatureRpcService featureRpcService;

    private FeatureChecker checker;

    @BeforeEach
    void setUp() {
        checker = new FeatureChecker();
        ReflectionTestUtils.setField(checker, "featureRpcService", featureRpcService);
        ReflectionTestUtils.setField(checker, "applicationName", APP);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("无租户上下文时直接返回空串且不调用 RPC")
    void getValueWithoutTenant() {
        assertThat(checker.getValue("f")).isEmpty();
        verifyNoInteractions(featureRpcService);
    }

    @Test
    @DisplayName("有租户上下文时按当前租户读取特性值")
    void getValueWithTenant() {
        TenantContextHolder.setTenantId(1L);
        when(featureRpcService.getValue(APP, 1L, "f")).thenReturn("v");

        assertThat(checker.getValue("f")).isEqualTo("v");
    }

    @Test
    @DisplayName("指定租户读取特性值")
    void getValueByTenant() {
        when(featureRpcService.getValue(APP, 2L, "f")).thenReturn("v2");

        assertThat(checker.getValue(2L, "f")).isEqualTo("v2");
    }

    @Test
    @DisplayName("isEnabled 忽略大小写，仅 true 为开启")
    void isEnabledParsesBoolean() {
        TenantContextHolder.setTenantId(1L);
        when(featureRpcService.getValue(APP, 1L, "on")).thenReturn("TRUE");
        when(featureRpcService.getValue(APP, 1L, "off")).thenReturn("false");
        when(featureRpcService.getValue(APP, 1L, "null")).thenReturn(null);

        assertThat(checker.isEnabled("on")).isTrue();
        assertThat(checker.isEnabled("off")).isFalse();
        assertThat(checker.isEnabled("null")).isFalse();
    }

    @Test
    @DisplayName("isEnabled(tenantId, name) 按指定租户判定")
    void isEnabledByTenant() {
        when(featureRpcService.getValue(APP, 2L, "on")).thenReturn("true");

        assertThat(checker.isEnabled(2L, "on")).isTrue();
        verify(featureRpcService).getValue(APP, 2L, "on");
    }
}
