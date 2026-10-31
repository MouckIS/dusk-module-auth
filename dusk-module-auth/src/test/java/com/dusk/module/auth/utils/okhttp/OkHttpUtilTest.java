package com.dusk.module.auth.utils.okhttp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OkHttpUtil} 单元测试：验证「忽略证书校验」的 SSL 上下文与主机名校验器。
 *
 * <p>这类工具类没有分支，但一旦实现被改坏（例如误用严格的 TrustManager）会直接影响
 * 测试环境的 HTTPS 联调，因此对契约做显式断言。</p>
 */
class OkHttpUtilTest {

    @Test
    @DisplayName("工具类可实例化（隐式构造器），方法全部为静态")
    void utilityClassHasImplicitConstructor() {
        assertThat(new OkHttpUtil()).isNotNull();
    }

    @Test
    @DisplayName("getIgnoreInitedSslContext：返回可用的已初始化 SSLContext")
    void returnsInitialisedSslContext() {
        SSLContext sslContext = OkHttpUtil.getIgnoreInitedSslContext();

        assertThat(sslContext).isNotNull();
        assertThat(sslContext.getProtocol()).isEqualTo("SSL");
        assertThat(sslContext.getSocketFactory()).isNotNull();
    }

    @Test
    @DisplayName("忽略证书的 TrustManager：客户端/服务端证书一律放行，受信颁发者为空数组")
    void ignoreTrustManagerAcceptsEverything() throws Exception {
        X509TrustManager trustManager = OkHttpUtil.IGNORE_SSL_TRUST_MANAGER_X509;

        trustManager.checkClientTrusted(new X509Certificate[]{}, "RSA");
        trustManager.checkServerTrusted(new X509Certificate[]{}, "RSA");

        assertThat(trustManager.getAcceptedIssuers()).isEmpty();
    }

    @Test
    @DisplayName("每次调用都返回新的已初始化 SSLContext（无共享可变状态）")
    void createsFreshSslContextPerCall() {
        SSLContext first = OkHttpUtil.getIgnoreInitedSslContext();
        SSLContext second = OkHttpUtil.getIgnoreInitedSslContext();

        assertThat(first).isNotSameAs(second);
        assertThat(first.getProtocol()).isEqualTo(second.getProtocol());
    }

    @Test
    @DisplayName("getIgnoreSslHostnameVerifier：任何主机名都校验通过")
    void ignoreHostnameVerifierAcceptsAnyHost() {
        HostnameVerifier verifier = OkHttpUtil.getIgnoreSslHostnameVerifier();

        assertThat(verifier.verify("any-host", (SSLSession) null)).isTrue();
        assertThat(verifier.verify(null, null)).isTrue();
    }
}
