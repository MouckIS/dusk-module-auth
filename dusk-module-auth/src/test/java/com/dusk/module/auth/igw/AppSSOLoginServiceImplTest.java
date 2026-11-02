package com.dusk.module.auth.igw;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.service.IUserService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AppSSOLoginServiceImpl} 单元测试，覆盖 i国网（IGW）单点登录的三段失败语义：
 *
 * <ol>
 *   <li>换取 accessToken 失败 → 通用异常，包装为「token获取失败」；</li>
 *   <li>解析 ticket 失败 → 同上；</li>
 *   <li>用户不存在（{@link BusinessException}）→ 提示到统一权限平台授权。</li>
 * </ol>
 *
 * <p>HTTP 调用通过 Mockito 静态打桩 {@link HttpUtil#createPost(String)} 拦截，
 * 不访问任何真实网络。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppSSOLoginServiceImplTest {

    private static final String ADDRESS = "https://igw.example.com/";
    private static final String APP_ID = "app-id";
    private static final String SECRET = "app-secret";
    private static final String SERVICE = "dusk";

    @Mock
    private IUserService userService;

    private AppSSOLoginServiceImpl service;

    /**
     * 构造可链式调用的 HttpRequest 替身：hutool 的 {@code contentType/addHeaders/timeout/body}
     * 都返回自身，Mockito 的 RETURNS_SELF 对泛型返回（{@code HttpBase}）不生效，因此显式打桩。
     */
    private static HttpRequest chainedRequest() {
        HttpRequest request = mock(HttpRequest.class);
        when(request.contentType(anyString())).thenReturn(request);
        when(request.addHeaders(any())).thenReturn(request);
        when(request.timeout(anyInt())).thenReturn(request);
        when(request.body(anyString())).thenReturn(request);
        return request;
    }

    private static HttpRequest requestReturning(String body) {
        HttpRequest request = chainedRequest();
        HttpResponse response = mock(HttpResponse.class);
        when(response.body()).thenReturn(body);
        when(request.execute()).thenReturn(response);
        return request;
    }

    @BeforeEach
    void setUp() {
        service = new AppSSOLoginServiceImpl();
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "iscAppId", APP_ID);
        ReflectionTestUtils.setField(service, "iscSecret", SECRET);
        ReflectionTestUtils.setField(service, "service", SERVICE);
        ReflectionTestUtils.setField(service, "address", ADDRESS);
    }

    @Test
    @DisplayName("登录成功：依次换取 accessToken 与用户信息，返回本系统 JWT")
    void loginSuccessReturnsToken() {
        HttpRequest tokenRequest = requestReturning("{\"code\":\"100001\",\"data\":{\"accessToken\":\"igw-token\"}}");
        HttpRequest userInfoRequest = requestReturning("{\"code\":\"100001\",\"data\":{\"account\":\"zhangsan\"}}");
        when(userService.generateTokenByUserName("zhangsan")).thenReturn("dusk-jwt");

        try (MockedStatic<HttpUtil> httpUtil = Mockito.mockStatic(HttpUtil.class)) {
            httpUtil.when(() -> HttpUtil.createPost(anyString())).thenReturn(tokenRequest, userInfoRequest);

            assertThat(service.login("ticket-1")).isEqualTo("dusk-jwt");
        }
    }

    @Test
    @DisplayName("换取 accessToken 失败：包装为「token获取失败」业务异常")
    void loginFailsWhenAccessTokenRejected() {
        HttpRequest tokenRequest = requestReturning("{\"code\":\"500\",\"message\":\"invalid secret\"}");

        try (MockedStatic<HttpUtil> httpUtil = Mockito.mockStatic(HttpUtil.class)) {
            httpUtil.when(() -> HttpUtil.createPost(anyString())).thenReturn(tokenRequest);

            assertThatThrownBy(() -> service.login("ticket-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("token获取失败")
                    .hasMessageContaining("invalid secret");
        }
    }

    @Test
    @DisplayName("解析票据失败：包装为「token获取失败」业务异常")
    void loginFailsWhenTicketRejected() {
        HttpRequest tokenRequest = requestReturning("{\"code\":\"100001\",\"data\":{\"accessToken\":\"igw-token\"}}");
        HttpRequest userInfoRequest = requestReturning("{\"code\":\"400\",\"message\":\"ticket expired\"}");

        try (MockedStatic<HttpUtil> httpUtil = Mockito.mockStatic(HttpUtil.class)) {
            httpUtil.when(() -> HttpUtil.createPost(anyString())).thenReturn(tokenRequest, userInfoRequest);

            assertThatThrownBy(() -> service.login("ticket-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("token获取失败")
                    .hasMessageContaining("ticket expired");
        }
    }

    @Test
    @DisplayName("本地用户不存在：BusinessException 被转换为「请在统一权限平台授权」")
    void loginFailsWhenLocalUserMissing() {
        HttpRequest tokenRequest = requestReturning("{\"code\":\"100001\",\"data\":{\"accessToken\":\"igw-token\"}}");
        HttpRequest userInfoRequest = requestReturning("{\"code\":\"100001\",\"data\":{\"account\":\"ghost\"}}");
        when(userService.generateTokenByUserName("ghost")).thenThrow(new BusinessException("用户不存在"));

        try (MockedStatic<HttpUtil> httpUtil = Mockito.mockStatic(HttpUtil.class)) {
            httpUtil.when(() -> HttpUtil.createPost(anyString())).thenReturn(tokenRequest, userInfoRequest);

            assertThatThrownBy(() -> service.login("ticket-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在，请在统一权限平台授权");
        }
    }

    @Test
    @DisplayName("网络异常：execute 抛出运行时异常时仍被包装为业务异常")
    void loginFailsOnTransportError() {
        HttpRequest tokenRequest = chainedRequest();
        when(tokenRequest.execute()).thenThrow(new IllegalStateException("connect timed out"));

        try (MockedStatic<HttpUtil> httpUtil = Mockito.mockStatic(HttpUtil.class)) {
            httpUtil.when(() -> HttpUtil.createPost(anyString())).thenReturn(tokenRequest);

            assertThatThrownBy(() -> service.login("ticket-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("token获取失败")
                    .hasMessageContaining("connect timed out");
        }
    }
}
