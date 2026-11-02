package com.dusk.module.auth.common.handler;

import com.dusk.common.core.model.UserContext;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.service.ICaptchaService;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DefaultAuthenticationSuccessHandler} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultAuthenticationSuccessHandlerTest {

    @Mock
    private TokenAuthManager tokenAuthManager;
    @Mock
    private ICaptchaService captchaService;

    private DefaultAuthenticationSuccessHandler handler;

    private static Authentication authentication(UserContext userContext) {
        return new UsernamePasswordAuthenticationToken(userContext, null, List.of());
    }

    @BeforeEach
    void setUp() {
        handler = new DefaultAuthenticationSuccessHandler();
        ReflectionTestUtils.setField(handler, "mapper", new ObjectMapper());
        ReflectionTestUtils.setField(handler, "tokenAuthManager", tokenAuthManager);
        ReflectionTestUtils.setField(handler, "captchaService", captchaService);
    }

    @Test
    @DisplayName("onAuthenticationSuccess：签发 token 写入响应并清理会话中的认证异常")
    void success_withSession() throws Exception {
        UserContext userContext = new UserContext();
        userContext.setId(1L);
        when(tokenAuthManager.generateToken(userContext)).thenReturn("token-123");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new RuntimeException("old"));
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication(userContext));

        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("token-123").contains("登录成功");
        assertThat(session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)).isNull();
        verify(captchaService).clearBuffer(request);
    }

    @Test
    @DisplayName("onAuthenticationSuccess：无会话时不抛异常仍返回 token")
    void success_withoutSession() throws Exception {
        UserContext userContext = new UserContext();
        userContext.setId(2L);
        when(tokenAuthManager.generateToken(userContext)).thenReturn("token-456");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication(userContext));

        assertThat(response.getContentAsString()).contains("token-456");
        verify(captchaService).clearBuffer(request);
    }
}
