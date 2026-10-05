package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.module.auth.common.config.AppAuthConfig;
import com.dusk.module.auth.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SsoTokenServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class SsoTokenServiceImplTest {

    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";
    private static final String SSO_KEY_PREFIX = "CRUX:AUTH:SSO:SM4:";
    private static final int TIME_DIFF_MINUTES = 5;

    @Mock
    private IUserService userService;
    @Mock
    private AppAuthConfig appAuthConfig;
    @Mock
    private RedisUtil<String> redisUtil;

    private SsoTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SsoTokenServiceImpl();
        ReflectionTestUtils.setField(service, "timeDiff", TIME_DIFF_MINUTES);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "appAuthConfig", appAuthConfig);
    }

    private void enableRedis() {
        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
    }

    private static String encrypt(String plain) {
        return SmUtil.sm4(HexUtil.decodeHex(SM4_KEY)).encryptHex(plain);
    }

    @Test
    @DisplayName("ssoSm4Token：Redis 不可用时直接校验时间戳并签发")
    void ssoSm4TokenWithoutRedisSignsToken() {
        when(appAuthConfig.getLoginEncryptKey()).thenReturn(SM4_KEY);
        when(userService.generateTokenByUserName("admin")).thenReturn("token-1");

        String token = service.ssoSm4Token(encrypt("admin|" + System.currentTimeMillis()));

        assertThat(token).isEqualTo("token-1");
    }

    @Test
    @DisplayName("ssoSm4Token：票据已被使用（重放）时抛出业务异常")
    void ssoSm4TokenRejectsReplay() {
        enableRedis();
        when(redisUtil.hasKey(SSO_KEY_PREFIX + "cipher")).thenReturn(true);

        assertThatThrownBy(() -> service.ssoSm4Token("cipher"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法请求");
    }

    @Test
    @DisplayName("ssoSm4Token：票据有效时写入一次性标记并签发")
    void ssoSm4TokenStoresOneTimeTicket() {
        enableRedis();
        when(redisUtil.hasKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        when(appAuthConfig.getLoginEncryptKey()).thenReturn(SM4_KEY);
        when(userService.generateTokenByUserName("admin")).thenReturn("token-2");

        String cipher = encrypt("admin|" + System.currentTimeMillis());
        String token = service.ssoSm4Token(cipher);

        assertThat(token).isEqualTo("token-2");
        verify(redisUtil).setCache(SSO_KEY_PREFIX + cipher, "", TIME_DIFF_MINUTES, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("ssoSm4Token：票据超时抛出业务异常")
    void ssoSm4TokenRejectsExpiredTicket() {
        enableRedis();
        when(redisUtil.hasKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        when(appAuthConfig.getLoginEncryptKey()).thenReturn(SM4_KEY);

        long expired = System.currentTimeMillis() - (TIME_DIFF_MINUTES + 5) * 60_000L;

        assertThatThrownBy(() -> service.ssoSm4Token(encrypt("admin|" + expired)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请求已过期");
    }

    @Test
    @DisplayName("ssoSm4Token：密文非法时抛出业务异常")
    void ssoSm4TokenRejectsInvalidCipher() {
        when(appAuthConfig.getLoginEncryptKey()).thenReturn(SM4_KEY);

        assertThatThrownBy(() -> service.ssoSm4Token("not-a-valid-cipher"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法请求");
    }
}
