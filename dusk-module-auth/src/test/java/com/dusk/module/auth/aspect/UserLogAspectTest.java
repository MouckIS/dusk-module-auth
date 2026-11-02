package com.dusk.module.auth.aspect;

import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.utils.UserContextUtils;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.enums.LoginLogType;
import com.dusk.module.auth.listener.LogInOutEvent;
import com.dusk.module.auth.repository.IUserRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link UserLogAspect} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserLogAspectTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private UserContextUtils userContextUtils;
    @Mock
    private ProceedingJoinPoint joinPoint;

    private UserLogAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new UserLogAspect();
        ReflectionTestUtils.setField(aspect, "eventPublisher", eventPublisher);
        ReflectionTestUtils.setField(aspect, "userRepository", userRepository);
        ReflectionTestUtils.setField(aspect, "userContextUtils", userContextUtils);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "junit-agent");
        request.setRemoteAddr("10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Bearer token"});
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("logLogOut：解析用户、补齐 IP/UA 并发布登出日志")
    void logLogOut_success() throws Throwable {
        UserContext userContext = new UserContext();
        userContext.setId(1L);
        User user = new User();
        user.setUserName("alice");
        when(userContextUtils.getUserContext(anyString())).thenReturn(userContext);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        aspect.logLogOut(joinPoint);

        verify(joinPoint).proceed();
        ArgumentCaptor<LogInOutEvent> captor = ArgumentCaptor.forClass(LogInOutEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        LogInOutEvent event = captor.getValue();
        assertThat(event.getLogType()).isEqualTo(LoginLogType.LOGIN_OUT);
        assertThat(event.getUserName()).isEqualTo("alice");
        assertThat(event.getIp()).isEqualTo("10.0.0.8");
        assertThat(event.getBrowserInfo()).isEqualTo("junit-agent");
        assertThat(event.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("logLogOut：token 为空时不解码用户但仍记录请求信息")
    void logLogOut_blankToken() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{""});

        aspect.logLogOut(joinPoint);

        verify(userContextUtils, never()).getUserContext(anyString());
        ArgumentCaptor<LogInOutEvent> captor = ArgumentCaptor.forClass(LogInOutEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getUserName()).isNull();
        assertThat(captor.getValue().getIp()).isEqualTo("10.0.0.8");
    }

    @Test
    @DisplayName("logLogOut：token 无法解析出用户时不查询用户")
    void logLogOut_userContextNull() throws Throwable {
        when(userContextUtils.getUserContext(anyString())).thenReturn(null);

        aspect.logLogOut(joinPoint);

        verify(userRepository, never()).findById(any());
        ArgumentCaptor<LogInOutEvent> captor = ArgumentCaptor.forClass(LogInOutEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getUserName()).isNull();
    }

    @Test
    @DisplayName("logLogOut：用户已被删除时仍发布日志")
    void logLogOut_userNotFound() throws Throwable {
        UserContext userContext = new UserContext();
        userContext.setId(2L);
        when(userContextUtils.getUserContext(anyString())).thenReturn(userContext);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        aspect.logLogOut(joinPoint);

        ArgumentCaptor<LogInOutEvent> captor = ArgumentCaptor.forClass(LogInOutEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getUserName()).isNull();
    }

    @Test
    @DisplayName("logLogOut：没有请求上下文时日志初始化异常被吞掉且业务继续执行")
    void logLogOut_fillLogFails() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        UserContext userContext = new UserContext();
        userContext.setId(3L);
        when(userContextUtils.getUserContext(anyString())).thenReturn(userContext);
        when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));

        assertThatCode(() -> aspect.logLogOut(joinPoint)).doesNotThrowAnyException();

        verify(joinPoint).proceed();
        verify(eventPublisher).publishEvent(any(LogInOutEvent.class));
    }

    @Test
    @DisplayName("logLogOut：业务异常时标记失败、截断消息并原样抛出")
    void logLogOut_proceedFails() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("proceed failed"));

        assertThatThrownBy(() -> aspect.logLogOut(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("proceed failed");

        ArgumentCaptor<LogInOutEvent> captor = ArgumentCaptor.forClass(LogInOutEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isSuccess()).isFalse();
        assertThat(captor.getValue().getMsg()).isEqualTo("proceed failed");
    }
}
