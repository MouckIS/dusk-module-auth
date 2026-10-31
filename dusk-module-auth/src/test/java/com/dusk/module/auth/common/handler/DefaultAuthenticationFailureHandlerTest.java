package com.dusk.module.auth.common.handler;

import com.dusk.common.core.exception.UserLoginException;
import com.dusk.common.core.jwt.exception.JwtExpiredTokenException;
import com.dusk.module.auth.service.ICaptchaService;
import com.dusk.module.auth.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DefaultAuthenticationFailureHandler} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultAuthenticationFailureHandlerTest {

    @Mock
    private ICaptchaService captchaService;

    private DefaultAuthenticationFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DefaultAuthenticationFailureHandler();
        ReflectionTestUtils.setField(handler, "mapper", new ObjectMapper());
        ReflectionTestUtils.setField(handler, "captchaService", captchaService);
    }

    @Test
    @DisplayName("onAuthenticationFailure：凭证错误且需要验证码时返回 200 与验证码错误码")
    void badCredentials_needCaptcha() throws Exception {
        when(captchaService.checkAndWriteError(any())).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new BadCredentialsException("密码错误"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString())
                .contains("\"code\":" + UserServiceImpl.CODE_USER_VALID)
                .contains("\"success\":true")
                .contains("密码错误");
        verify(captchaService).checkAndWriteError(any());
    }

    @Test
    @DisplayName("onAuthenticationFailure：凭证错误但无需验证码时 success 为 false")
    void badCredentials_noCaptcha() throws Exception {
        when(captchaService.checkAndWriteError(any())).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new BadCredentialsException("密码错误"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString())
                .contains("\"success\":false")
                .doesNotContain("\"code\":" + UserServiceImpl.CODE_USER_VALID);
    }

    @Test
    @DisplayName("onAuthenticationFailure：用户名不存在走与凭证错误相同的处理分支")
    void usernameNotFound() throws Exception {
        when(captchaService.checkAndWriteError(any())).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new UsernameNotFoundException("用户不存在"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("用户不存在");
        verify(captchaService).checkAndWriteError(any());
    }

    @Test
    @DisplayName("onAuthenticationFailure：JWT 过期返回 401 与过期提示")
    void jwtExpired() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new JwtExpiredTokenException("expired", new RuntimeException()));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains("登陆已过期，请重新登陆");
    }

    @Test
    @DisplayName("onAuthenticationFailure：UserLoginException 返回 200 并带业务错误码")
    void userLoginException() throws Exception {
        UserLoginException exception = new UserLoginException("账号锁定");
        exception.setCode(2001);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, exception);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString())
                .contains("\"code\":2001")
                .contains("账号锁定")
                .contains("\"success\":true");
    }

    @Test
    @DisplayName("onAuthenticationFailure：其他认证异常返回 401 与无效访问提示")
    void otherAuthenticationException() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new AuthenticationServiceException("服务异常"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains("无效访问：服务异常")
                .doesNotContain("登陆已过期");
    }
}
