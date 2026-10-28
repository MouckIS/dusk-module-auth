package com.dusk.module.auth.registry.security;

import com.dusk.module.auth.registry.config.RegistryProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ServiceSecretVerifier} 单元测试，覆盖《权限优化方案-整理版》3.5 的服务间密钥语义。
 *
 * <p>重点在<b>失效方向</b>：未配置密钥时默认必须拒绝（fail-closed），
 * 只有显式关闭强制校验才允许空密钥放行。</p>
 */
class ServiceSecretVerifierTest {

    @Test
    @DisplayName("已配置密钥：匹配通过，缺失或不匹配一律拒绝")
    void shouldMatchConfiguredSecret() {
        assertThat(ServiceSecretVerifier.matches("s3cret", "s3cret", true)).isTrue();
        assertThat(ServiceSecretVerifier.matches("s3cret", " s3cret ", true)).isTrue();
        assertThat(ServiceSecretVerifier.matches("s3cret", "other", true)).isFalse();
        assertThat(ServiceSecretVerifier.matches("s3cret", "", true)).isFalse();
        assertThat(ServiceSecretVerifier.matches("s3cret", null, true)).isFalse();
        assertThat(ServiceSecretVerifier.matches("s3cret", null, false)).isFalse();
    }

    @Test
    @DisplayName("未配置密钥：默认拒绝（fail-closed），显式关闭强制校验才放行")
    void shouldRejectWhenSecretNotConfiguredByDefault() {
        assertThat(ServiceSecretVerifier.matches(null, null, true)).isFalse();
        assertThat(ServiceSecretVerifier.matches("   ", "anything", true)).isFalse();
        assertThat(ServiceSecretVerifier.matches(null, null, false)).isTrue();
    }

    @Test
    @DisplayName("实例入口读取配置：密钥与开关组合行为一致")
    void shouldFollowProperties() {
        RegistryProperties properties = new RegistryProperties();

        RegistryProperties defaultProperties = new RegistryProperties();
        defaultProperties.setRequireServiceSecret(true);
        assertThat(new ServiceSecretVerifier(defaultProperties).isAuthorized(null)).isFalse();

        properties.setRequireServiceSecret(false);
        assertThat(new ServiceSecretVerifier(properties).isAuthorized(null)).isTrue();

        properties.setServiceSecret("abc");
        assertThat(new ServiceSecretVerifier(properties).isAuthorized("abc")).isTrue();
        assertThat(new ServiceSecretVerifier(properties).isAuthorized(null)).isFalse();
    }
}
