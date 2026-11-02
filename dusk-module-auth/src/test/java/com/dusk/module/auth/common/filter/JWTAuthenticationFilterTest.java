package com.dusk.module.auth.common.filter;

import com.dusk.common.core.auditlog.SysLogEvent;
import com.dusk.common.core.dto.AuditLogDto;
import com.dusk.module.auth.enums.LoginLogType;
import com.dusk.module.auth.listener.LogInOutEvent;
import com.dusk.module.auth.service.ICaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link JWTAuthenticationFilter} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JWTAuthenticationFilterTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AuthenticationSuccessHandler successHandler;
    @Mock
    private AuthenticationFailureHandler failureHandler;
    @Mock
    private ICaptchaService captchaService;
    @Mock
    private ApplicationEventPublisher publisher;

    private JWTAuthenticationFilter filter;

    private static MockHttpServletRequest request(String json) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setContent(json.getBytes(StandardCharsets.UTF_8));
        request.addHeader("User-Agent", "junit-agent");
        request.setRemoteAddr("10.1.2.3");
        return request;
    }

    @BeforeEach
    void setUp() {
        filter = new JWTAuthenticationFilter("/login", successHandler, failureHandler,
                new ObjectMapper(), captchaService, publisher);
        filter.setAuthenticationManager(authenticationManager);
    }

    @Test
    @DisplayName("attemptAuthentication：验证码通过且认证成功时返回认证结果并发布审计与登录日志")
    void attemptAuthentication_success() throws Exception {
        when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
        UsernamePasswordAuthenticationToken authenticated =
                new UsernamePasswordAuthenticationToken("alice", "pwd", List.of());
        when(authenticationManager.authenticate(any())).thenReturn(authenticated);

        Authentication result = filter.attemptAuthentication(
                request("{\"username\":\"alice\",\"password\":\"secret\"}"), new MockHttpServletResponse());

        assertThat(result).isSameAs(authenticated);
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(2)).publishEvent(events.capture());

        SysLogEvent sysLogEvent = (SysLogEvent) events.getAllValues().get(0);
        AuditLogDto auditLog = sysLogEvent.sysLog();
        assertThat(auditLog.getMethodName()).isEqualTo("login");
        assertThat(auditLog.getServiceName()).isEqualTo("login");
        assertThat(auditLog.getBrowserInfo()).isEqualTo("junit-agent");
        assertThat(auditLog.getClientIpAddress()).isEqualTo("10.1.2.3");
        assertThat(auditLog.getParameters()).contains("\"password\":\"***\"").contains("alice");
        assertThat(auditLog.getException()).isNull();

        LogInOutEvent logInOutEvent = (LogInOutEvent) events.getAllValues().get(1);
        assertThat(logInOutEvent.getLogType()).isEqualTo(LoginLogType.LOGIN_IN);
        assertThat(logInOutEvent.isSuccess()).isTrue();
        assertThat(logInOutEvent.getUserName()).isEqualTo("alice");
        assertThat(logInOutEvent.getIp()).isEqualTo("10.1.2.3");
        assertThat(logInOutEvent.getBrowserInfo()).isEqualTo("junit-agent");
    }

    @Test
    @DisplayName("attemptAuthentication：验证码校验失败抛出 BadCredentialsException 并记录失败日志")
    void attemptAuthentication_captchaRejected() {
        when(captchaService.verifyCaptcha(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("{\"username\":\"alice\",\"password\":\"secret\"}"), new MockHttpServletResponse()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("验证码错误");

        verify(authenticationManager, never()).authenticate(any());
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(2)).publishEvent(events.capture());
        LogInOutEvent logInOutEvent = (LogInOutEvent) events.getAllValues().get(1);
        assertThat(logInOutEvent.isSuccess()).isFalse();
        assertThat(logInOutEvent.getMsg()).isEqualTo("验证码错误");
        SysLogEvent sysLogEvent = (SysLogEvent) events.getAllValues().get(0);
        assertThat(sysLogEvent.sysLog().getException()).contains("BadCredentialsException");
    }

    @Test
    @DisplayName("attemptAuthentication：用户名为空时抛出 BadCredentialsException")
    void attemptAuthentication_blankUsername() {
        when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("{\"password\":\"secret\"}"), new MockHttpServletResponse()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("帐户名或密码不能为空");
    }

    @Test
    @DisplayName("attemptAuthentication：密码为空时抛出 BadCredentialsException")
    void attemptAuthentication_blankPassword() {
        when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("{\"username\":\"alice\"}"), new MockHttpServletResponse()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("帐户名或密码不能为空");
    }

    @Test
    @DisplayName("attemptAuthentication：请求体不是对象时按凭证缺失处理")
    void attemptAuthentication_mismatchedInput() {
        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("[]"), new MockHttpServletResponse()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("帐户名或密码不能为空");

        verify(captchaService, never()).verifyCaptcha(any(), any());
        verify(publisher, times(2)).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("attemptAuthentication：认证管理器抛异常时原样抛出并记录异常")
    void attemptAuthentication_authenticationFails() {
        when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("密码错误"));

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("{\"username\":\"alice\",\"password\":\"secret\"}"), new MockHttpServletResponse()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("密码错误");

        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(2)).publishEvent(events.capture());
        assertThat(((LogInOutEvent) events.getAllValues().get(1)).getMsg()).isEqualTo("密码错误");
    }

    @Test
    @DisplayName("successfulAuthentication：委托成功处理器")
    void successfulAuthentication_delegates() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "pwd", List.of());

        filter.successfulAuthentication(new MockHttpServletRequest(), new MockHttpServletResponse(), null, authentication);

        verify(successHandler).onAuthenticationSuccess(any(), any(), eq(authentication));
    }

    @Test
    @DisplayName("unsuccessfulAuthentication：清理安全上下文并委托失败处理器")
    void unsuccessfulAuthentication_clearsContext() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "pwd", List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        BadCredentialsException exception = new BadCredentialsException("bad");

        filter.unsuccessfulAuthentication(new MockHttpServletRequest(), new MockHttpServletResponse(), exception);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(failureHandler).onAuthenticationFailure(any(), any(), eq(exception));
    }
}
