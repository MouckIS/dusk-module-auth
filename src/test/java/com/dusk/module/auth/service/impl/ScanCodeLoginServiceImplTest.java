package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.response.BaseApiResult;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.module.auth.common.manage.TokenAuthManager;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ScanCodeLoginServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class ScanCodeLoginServiceImplTest {

    private static final String KEY_PREFIX = "CRUX:LOGIN:SCANCODE:";

    @Mock
    private RedisUtil<String> redisUtil;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private TokenAuthManager tokenAuthManager;

    private ScanCodeLoginServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScanCodeLoginServiceImpl();
    }

    private void enableRedis() {
        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
    }

    @Test
    @DisplayName("getLoginKey：未启用 Redis 时抛出业务异常")
    void getLoginKeyThrowsWithoutRedis() {
        assertThatThrownBy(service::getLoginKey).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getLoginKey：启用 Redis 时生成并缓存登录 key")
    void getLoginKeyStoresKeyWithRedis() {
        enableRedis();
        String key = service.getLoginKey();
        assertThat(key).isNotBlank();
        verify(redisUtil).setCache(KEY_PREFIX + key, "", 5L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("getToken：未启用 Redis 时抛出业务异常")
    void getTokenThrowsWithoutRedis() {
        assertThatThrownBy(() -> service.getToken("key")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getToken：缓存未命中时返回 1001")
    void getTokenReturns1001OnCacheMiss() {
        enableRedis();
        when(redisUtil.getCache(KEY_PREFIX + "key")).thenReturn(null);

        BaseApiResult<String> result = service.getToken("key");

        assertThat(result.getCode()).isEqualTo(1001);
    }

    @Test
    @DisplayName("getToken：缓存命中时返回 token 并删除缓存")
    void getTokenDeletesCacheOnHit() {
        enableRedis();
        when(redisUtil.getCache(KEY_PREFIX + "key")).thenReturn("token-1");

        BaseApiResult<String> result = service.getToken("key");

        assertThat(result.getData()).isEqualTo("token-1");
        verify(redisUtil).deleteCache(KEY_PREFIX + "key");
    }

    @Test
    @DisplayName("login：未启用 Redis 时抛出业务异常")
    void loginThrowsWithoutRedis() {
        assertThatThrownBy(() -> service.login("key")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("login：启用 Redis 时写入扫码 token")
    void loginStoresTokenWithRedis() {
        enableRedis();
        ReflectionTestUtils.setField(service, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(service, "tokenAuthManager", tokenAuthManager);

        when(securityUtils.getCurrentUser()).thenReturn(new UserContext());
        when(tokenAuthManager.generateToken(org.mockito.ArgumentMatchers.any(UserContext.class), eq(365L), eq(TimeUnit.DAYS)))
                .thenReturn("token-2");

        service.login("key");

        verify(redisUtil).setCache(KEY_PREFIX + "key", "token-2", 5L, TimeUnit.MINUTES);
    }
}
