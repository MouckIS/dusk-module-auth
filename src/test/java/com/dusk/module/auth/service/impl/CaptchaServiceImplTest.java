package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.response.BaseApiResult;
import com.dusk.module.auth.dto.captcha.CaptchaInputDto;
import com.dusk.module.auth.dto.captcha.CaptchaOutDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CaptchaServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    private static final String CAPTCHA_KEY_PREFIX = "CRUX:LOGIN:CAPTCHA:KEY:";
    private static final String NEED_CAPTCHA_PREFIX = "CRUX:LOGIN:CAPTCHA:IP:";
    private static final String SEND_COUNT_PREFIX = "CRUX:IP:SEND:";

    @Mock
    private RedisUtil<Object> redisUtil;

    private CaptchaServiceImpl service;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        service = new CaptchaServiceImpl();
        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
        request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
    }

    @Test
    @DisplayName("getCaptcha：生成验证码并缓存 5 分钟")
    void getCaptchaStoresCaptchaInCache() {
        CaptchaOutDto dto = service.getCaptcha();

        assertThat(dto.getKey()).isNotBlank();
        assertThat(dto.getImageBase64()).isNotBlank();
        verify(redisUtil).setCache(anyString(), anyString(), anyLong(), org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("verifyCaptcha：当前实现恒返回 true")
    void verifyCaptchaAlwaysTrue() {
        assertThat(service.verifyCaptcha(new CaptchaInputDto(), request)).isTrue();
    }

    @Test
    @DisplayName("verifyCaptchaSendMobile：无发送计数时直接放行")
    void verifyCaptchaSendMobilePassesWhenNoCache() {
        when(redisUtil.getCache(anyString())).thenReturn(null);

        assertThat(service.verifyCaptchaSendMobile(new CaptchaInputDto(), request)).isTrue();
    }

    @Test
    @DisplayName("verifyCaptchaSendMobile：发送次数未超限时放行")
    void verifyCaptchaSendMobilePassesBelowThreshold() {
        when(redisUtil.getCache(anyString())).thenReturn("10");

        assertThat(service.verifyCaptchaSendMobile(new CaptchaInputDto(), request)).isTrue();
    }

    @Test
    @DisplayName("verifyCaptchaSendMobile：超限且验证码错误时抛出业务异常")
    void verifyCaptchaSendMobileThrowsWhenCaptchaInvalid() {
        when(redisUtil.getCache(anyString())).thenReturn("31");

        CaptchaInputDto input = new CaptchaInputDto();
        input.setKey("key");
        input.setCaptcha("abc");
        // 私有 verifyCaptcha 中 compare 为 null，返回 false
        when(redisUtil.getCache(CAPTCHA_KEY_PREFIX + "key")).thenReturn(null);

        assertThatThrownBy(() -> service.verifyCaptchaSendMobile(input, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("verifyCaptchaSendMobile：超限但验证码正确时放行")
    void verifyCaptchaSendMobilePassesWhenCaptchaValid() {
        when(redisUtil.getCache(anyString())).thenReturn("31");
        when(redisUtil.getCache(CAPTCHA_KEY_PREFIX + "key")).thenReturn("abc");

        CaptchaInputDto input = new CaptchaInputDto();
        input.setKey("key");
        input.setCaptcha("abc");

        assertThat(service.verifyCaptchaSendMobile(input, request)).isTrue();
        verify(redisUtil).deleteCache(CAPTCHA_KEY_PREFIX + "key");
    }

    @Test
    @DisplayName("verifyCaptchaSendMobile：超限但 key/captcha 为空时判定为失败")
    void verifyCaptchaSendMobileFailsWhenCredentialsBlank() {
        when(redisUtil.getCache(anyString())).thenReturn("31");

        assertThatThrownBy(() -> service.verifyCaptchaSendMobile(new CaptchaInputDto(), request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("verifyCaptchaSendMobile：key 非空但 captcha 为空时判定为失败")
    void verifyCaptchaSendMobileFailsWhenCaptchaEmpty() {
        when(redisUtil.getCache(anyString())).thenReturn("31");

        CaptchaInputDto input = new CaptchaInputDto();
        input.setKey("key");
        input.setCaptcha("");

        assertThatThrownBy(() -> service.verifyCaptchaSendMobile(input, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("setMobileSendCaptchaCount：自增计数并设置 60 秒过期")
    void setMobileSendCaptchaCountIncrements() {
        service.setMobileSendCaptchaCount(request);

        verify(redisUtil).setExpire(anyString(), org.mockito.ArgumentMatchers.eq(60L), org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("checkAndWriteError：当前实现恒返回 false")
    void checkAndWriteErrorReturnsFalse() {
        assertThat(service.checkAndWriteError(request)).isFalse();
    }

    @Test
    @DisplayName("checkNeedCaptcha：缓存为空时返回 false")
    void checkNeedCaptchaReturnsFalseWhenAbsent() {
        when(redisUtil.getCache(anyString())).thenReturn(null);

        assertThat(service.checkNeedCaptcha(request)).isFalse();
    }

    @Test
    @DisplayName("checkNeedCaptcha：缓存存在时返回 true")
    void checkNeedCaptchaReturnsTrueWhenPresent() {
        when(redisUtil.getCache(anyString())).thenReturn("1");

        assertThat(service.checkNeedCaptcha(request)).isTrue();
    }

    @Test
    @DisplayName("clearBuffer：清空错误计数与验证码标记")
    void clearBufferDeletesBothKeys() {
        service.clearBuffer(request);

        verify(redisUtil, org.mockito.Mockito.times(2)).deleteCache(anyString());
    }

    @Test
    @DisplayName("verifyCaptchaSendMobile：BaseApiResult 常量可正常构造")
    void baseApiResultConstantsAvailable() {
        BaseApiResult result = new BaseApiResult();
        result.setCode(1004);
        assertThat(result.getCode()).isEqualTo(1004);
    }
}
