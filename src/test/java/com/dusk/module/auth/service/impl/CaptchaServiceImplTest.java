package com.dusk.module.auth.service.impl;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.response.BaseApiResult;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.dto.captcha.CaptchaInputDto;
import com.dusk.module.auth.dto.captcha.CaptchaOutDto;
import com.dusk.module.auth.feature.LoginFeatureProvider;
import com.dusk.module.auth.service.IFeatureChecker;
import com.dusk.module.auth.util.TestDataBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证码服务单元测试 - 100%分支覆盖
 *
 * @author kefuming
 * @date 2026-02-28
 */
@DisplayName("验证码服务测试")
@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    @Mock
    private RedisUtil<Object> redisUtil;

    @Mock
    private IFeatureChecker featureChecker;

    @Mock
    private HttpServletRequest mockRequest;

    @InjectMocks
    private CaptchaServiceImpl captchaService;

    private static final String TEST_IP = "192.168.1.100";
    private static final String REDIS_KEY_CAPTCHA_PREFIX = "CRUX:LOGIN:CAPTCHA:KEY:";
    private static final String REDIS_KEY_NEED_CAPTCHA_PREFIX = "CRUX:LOGIN:CAPTCHA:IP:";
    private static final String REDIS_KEY_CAPTCHA_ERROR_COUNT_PREFIX = "CRUX:LOGIN:CAPTCHA:ERROR:IP:";
    private static final String REDIS_KEY_IP_SEND_KEY_PREFIX = "CRUX:IP:SEND:";

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @DisplayName("getCaptcha - 获取验证码成功")
    @Test
    void testGetCaptcha() {
        // Arrange
        try (MockedStatic<cn.hutool.core.util.IdUtil> mocked = mockStatic(cn.hutool.core.util.IdUtil.class)) {
            mocked.when(() -> cn.hutool.core.util.IdUtil.simpleUUID()).thenReturn("test-uuid-123");

            // Act
            CaptchaOutDto result = captchaService.getCaptcha();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getKey()).isEqualTo("test-uuid-123");
            assertThat(result.getImageBase64()).isNotEmpty();

            // Verify Redis缓存操作
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
            ArgumentCaptor<Integer> timeoutCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

            verify(redisUtil, times(1)).setCache(keyCaptor.capture(), valueCaptor.capture(),
                    timeoutCaptor.capture(), unitCaptor.capture());

            assertThat(keyCaptor.getValue()).startsWith(REDIS_KEY_CAPTCHA_PREFIX);
            assertThat(timeoutCaptor.getValue()).isEqualTo(5);
            assertThat(unitCaptor.getValue()).isEqualTo(TimeUnit.MINUTES);
        }
    }

    @DisplayName("verifyCaptcha - 不需要验证码时返回true")
    @Test
    void testVerifyCaptcha_NoCaptchaNeeded() {
        // Arrange
        CaptchaInputDto input = TestDataBuilder.buildCaptchaInputDto();
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn(null);

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.verifyCaptcha(input, mockRequest);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @DisplayName("verifyCaptcha - 需要验证码且验证成功")
    @Test
    void testVerifyCaptcha_CaptchaNeeded_Success() {
        // Arrange
        CaptchaInputDto input = TestDataBuilder.buildCaptchaInputDto("test-key-123", "12345");
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn("1");
        when(redisUtil.getCache(REDIS_KEY_CAPTCHA_PREFIX + "test-key-123")).thenReturn("12345");

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.verifyCaptcha(input, mockRequest);

            // Assert
            assertThat(result).isTrue();
            verify(redisUtil).deleteCache(REDIS_KEY_CAPTCHA_PREFIX + "test-key-123");
        }
    }

    @DisplayName("verifyCaptcha - 需要验证码但验证失败（无缓存）")
    @Test
    void testVerifyCaptcha_CaptchaNeeded_Failed_NoCacheFound() {
        // Arrange
        CaptchaInputDto input = TestDataBuilder.buildCaptchaInputDto("test-key-123", "12345");
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn("1");
        when(redisUtil.getCache(REDIS_KEY_CAPTCHA_PREFIX + "test-key-123")).thenReturn(null);

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.verifyCaptcha(input, mockRequest);

            // Assert
            assertThat(result).isFalse();
            verify(redisUtil, never()).deleteCache(anyString());
        }
    }

    @DisplayName("verifyCaptcha - 需要验证码但验证失败（验证码不匹配）")
    @Test
    void testVerifyCaptcha_CaptchaNeeded_Failed_MismatchCode() {
        // Arrange
        CaptchaInputDto input = TestDataBuilder.buildCaptchaInputDto("test-key-123", "12345");
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn("1");
        when(redisUtil.getCache(REDIS_KEY_CAPTCHA_PREFIX + "test-key-123")).thenReturn("54321");

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.verifyCaptcha(input, mockRequest);

            // Assert
            assertThat(result).isFalse();
            verify(redisUtil).deleteCache(REDIS_KEY_CAPTCHA_PREFIX + "test-key-123");
        }
    }

    @DisplayName("verifyCaptcha - 空的key或验证码")
    @Test
    void testVerifyCaptcha_EmptyKeyOrCaptcha() {
        // Arrange
        CaptchaInputDto input = TestDataBuilder.buildCaptchaInputDto("", "");
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn("1");

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.verifyCaptcha(input, mockRequest);

            // Assert
            assertThat(result).isFalse();
            verify(redisUtil, never()).deleteCache(anyString());
        }
    }

    @DisplayName("verifyCaptchaSendMobile - 没有超过发送限制")
    @Test
    void testVerifyCaptchaSendMobile_NotExceedLimit() {
        // Arrange
        CaptchaInputDto input = TestDataBuilder.buildCaptchaInputDto();
        when(redisUtil.getCache(REDIS_KEY_IP_SEND_KEY_PREFIX + TEST_IP)).thenReturn(null);

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.verifyCaptchaSendMobile(input, mockRequest);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @DisplayName("verifyCaptchaSendMobile - 超过发送限制且验证码验证成功")
    @Test
    void testVerifyCaptchaSendMobile_ExceedLimit_VerifySuccess() {
        // Arrange
        CaptchaInputDto input = TestDataBuilder.buildCaptchaInputDto("test-key-123", "12345");
        when(redisUtil.getCache(REDIS_KEY_IP_SEND_KEY_PREFIX + TEST_IP)).thenReturn(31); // 超过30个限制
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn(null);
        when(redisUtil.getCache(REDIS_KEY_CAPTCHA_PREFIX + "test-key-123")).thenReturn("12345");

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.verifyCaptchaSendMobile(input, mockRequest);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @DisplayName("verifyCaptchaSendMobile - 超过发送限制且验证码验证失败")
    @Test
    void testVerifyCaptchaSendMobile_ExceedLimit_VerifyFailed() {
        // Arrange
        CaptchaInputDto input = TestDataBuilder.buildCaptchaInputDto("test-key-123", "wrong");
        when(redisUtil.getCache(REDIS_KEY_IP_SEND_KEY_PREFIX + TEST_IP)).thenReturn(31);
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn(null);
        when(redisUtil.getCache(REDIS_KEY_CAPTCHA_PREFIX + "test-key-123")).thenReturn("12345");

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act & Assert
            assertThatThrownBy(() -> captchaService.verifyCaptchaSendMobile(input, mockRequest))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @DisplayName("setMobileSendCaptchaCount - 设置手机发送验证码计数")
    @Test
    void testSetMobileSendCaptchaCount() {
        // Arrange
        when(redisUtil.increment(anyString(), anyLong())).thenReturn(1L);

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            captchaService.setMobileSendCaptchaCount(mockRequest);

            // Assert
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> incrementCaptor = ArgumentCaptor.forClass(Long.class);
            verify(redisUtil).increment(keyCaptor.capture(), incrementCaptor.capture());

            assertThat(keyCaptor.getValue()).isEqualTo(REDIS_KEY_IP_SEND_KEY_PREFIX + TEST_IP);
            assertThat(incrementCaptor.getValue()).isEqualTo(1L);

            verify(redisUtil).setExpire(REDIS_KEY_IP_SEND_KEY_PREFIX + TEST_IP, 60, TimeUnit.SECONDS);
        }
    }

    @DisplayName("checkAndWriteError - IP已需要验证码")
    @Test
    void testCheckAndWriteError_AlreadyNeedCaptcha() {
        // Arrange
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn("1");

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.checkAndWriteError(mockRequest);

            // Assert
            assertThat(result).isTrue();
            verify(redisUtil).setCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP, "1", 1, TimeUnit.HOURS);
        }
    }

    @DisplayName("checkAndWriteError - 错误次数未达到最大值")
    @Test
    void testCheckAndWriteError_NotReachMaxCount() {
        // Arrange
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn(null);
        when(redisUtil.increment(REDIS_KEY_CAPTCHA_ERROR_COUNT_PREFIX + TEST_IP, 1)).thenReturn(1L);
        TenantContextHolder.setTenantId(null);

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.checkAndWriteError(mockRequest);

            // Assert
            assertThat(result).isFalse();
            verify(redisUtil).setExpire(REDIS_KEY_CAPTCHA_ERROR_COUNT_PREFIX + TEST_IP, 2, TimeUnit.MINUTES);
        }
    }

    @DisplayName("checkAndWriteError - 错误次数达到最大值，无租户")
    @Test
    void testCheckAndWriteError_ReachMaxCount_NoTenant() {
        // Arrange
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn(null);
        when(redisUtil.increment(REDIS_KEY_CAPTCHA_ERROR_COUNT_PREFIX + TEST_IP, 1)).thenReturn(3L);
        TenantContextHolder.setTenantId(null);

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.checkAndWriteError(mockRequest);

            // Assert
            assertThat(result).isTrue();
            verify(redisUtil).setCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP, "1", 1, TimeUnit.HOURS);
            verify(featureChecker, never()).getValue(anyString());
        }
    }

    @DisplayName("checkAndWriteError - 错误次数达到特性配置的最大值，有租户")
    @Test
    void testCheckAndWriteError_ReachMaxCount_WithTenant() {
        // Arrange
        TenantContextHolder.setTenantId(1L);
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn(null);
        when(redisUtil.increment(REDIS_KEY_CAPTCHA_ERROR_COUNT_PREFIX + TEST_IP, 1)).thenReturn(5L);
        when(featureChecker.getValue(LoginFeatureProvider.APP_LOGIN_MAX_ERROR)).thenReturn("5");

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.checkAndWriteError(mockRequest);

            // Assert
            assertThat(result).isTrue();
            verify(redisUtil).setCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP, "1", 1, TimeUnit.HOURS);
            verify(featureChecker).getValue(LoginFeatureProvider.APP_LOGIN_MAX_ERROR);
        }
    }

    @DisplayName("checkNeedCaptcha - IP需要验证码")
    @Test
    void testCheckNeedCaptcha_NeedCaptcha() {
        // Arrange
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn("1");

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.checkNeedCaptcha(mockRequest);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @DisplayName("checkNeedCaptcha - IP不需要验证码")
    @Test
    void testCheckNeedCaptcha_NoNeedCaptcha() {
        // Arrange
        when(redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP)).thenReturn(null);

        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            boolean result = captchaService.checkNeedCaptcha(mockRequest);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @DisplayName("clearBuffer - 清除缓存")
    @Test
    void testClearBuffer() {
        // Arrange
        try (MockedStatic<JakartaServletUtil> mocked = mockStatic(JakartaServletUtil.class)) {
            mocked.when(() -> JakartaServletUtil.getClientIP(mockRequest)).thenReturn(TEST_IP);

            // Act
            captchaService.clearBuffer(mockRequest);

            // Assert
            verify(redisUtil).deleteCache(REDIS_KEY_CAPTCHA_ERROR_COUNT_PREFIX + TEST_IP);
            verify(redisUtil).deleteCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + TEST_IP);
        }
    }
}

