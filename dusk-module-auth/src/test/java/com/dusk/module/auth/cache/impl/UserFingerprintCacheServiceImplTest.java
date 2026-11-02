package com.dusk.module.auth.cache.impl;

import com.dusk.common.core.redis.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserFingerprintCacheServiceImpl} 单元测试：验证用户指纹序号在 Redis 中的读写与 key 约定。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserFingerprintCacheServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final String REDIS_KEY = "CRUX:AUTH:FINGERPRINT:USERSEQ:7";

    @Mock
    private RedisUtil<Integer> redisUtil;

    private UserFingerprintCacheServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserFingerprintCacheServiceImpl();
        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
    }

    @Test
    @DisplayName("getUserSeq：按约定 key 读取序号")
    void getUserSeqReadsCache() {
        when(redisUtil.getCache(REDIS_KEY)).thenReturn(3);

        assertThat(service.getUserSeq(USER_ID)).isEqualTo(3);
        verify(redisUtil).getCache(REDIS_KEY);
    }

    @Test
    @DisplayName("getUserSeq：缓存未命中返回 null（不放大为默认值）")
    void getUserSeqReturnsNullOnMiss() {
        when(redisUtil.getCache(REDIS_KEY)).thenReturn(null);

        assertThat(service.getUserSeq(USER_ID)).isNull();
    }

    @Test
    @DisplayName("saveUserSeq：按约定 key 写入序号")
    void saveUserSeqWritesCache() {
        service.saveUserSeq(USER_ID, 9);

        verify(redisUtil).setCache(REDIS_KEY, 9);
    }
}
