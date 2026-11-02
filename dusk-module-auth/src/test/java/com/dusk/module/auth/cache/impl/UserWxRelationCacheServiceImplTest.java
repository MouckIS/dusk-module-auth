package com.dusk.module.auth.cache.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link UserWxRelationCacheServiceImpl} 单元测试。
 *
 * <p>该实现允许 {@code RedisTemplate} 注入失败（{@code @Autowired(required = false)}），
 * 因此所有方法都必须对 {@code redisTemplate == null} 的降级路径负责：写操作静默跳过、读操作返回 null。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserWxRelationCacheServiceImplTest {

    private static final Long USER_ID = 42L;
    private static final String REDIS_KEY = "CRUX:AUTH:WX:RELATION:42";
    private static final long EXPIRE_DAYS = 30L;

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private UserWxRelationCacheServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserWxRelationCacheServiceImpl();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("saveWxRelation：写入单个 appId→openId 并刷新 30 天过期时间")
    void saveWxRelationWritesHashAndExpire() {
        service.saveWxRelation(USER_ID, "wx-app", "open-id");

        verify(hashOperations).put(REDIS_KEY, "wx-app", "open-id");
        verify(redisTemplate).expire(REDIS_KEY, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("saveWxRelationMap：批量写入并刷新过期时间")
    void saveWxRelationMapWritesAllAndExpire() {
        Map<String, String> relations = new LinkedHashMap<>();
        relations.put("app1", "open1");
        relations.put("app2", "open2");

        service.saveWxRelationMap(USER_ID, relations);

        verify(hashOperations).putAll(REDIS_KEY, relations);
        verify(redisTemplate).expire(REDIS_KEY, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("getWxRelation：命中时返回 openId 并续期")
    void getWxRelationReturnsValueAndRenewsTtl() {
        when(hashOperations.get(REDIS_KEY, "wx-app")).thenReturn("open-id");

        assertThat(service.getWxRelation(USER_ID, "wx-app")).isEqualTo("open-id");
        verify(redisTemplate).expire(REDIS_KEY, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("getWxRelation：未命中时返回 null 并续期")
    void getWxRelationReturnsNullWhenMissing() {
        when(hashOperations.get(REDIS_KEY, "wx-app")).thenReturn(null);

        assertThat(service.getWxRelation(USER_ID, "wx-app")).isNull();
        verify(redisTemplate).expire(REDIS_KEY, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("getWxRelationMap：返回全部绑定并续期")
    void getWxRelationMapReturnsEntries() {
        Map<Object, Object> entries = Map.of("app1", "open1");
        when(hashOperations.entries(REDIS_KEY)).thenReturn(entries);

        assertThat(service.getWxRelationMap(USER_ID)).containsEntry("app1", "open1");
        verify(redisTemplate).expire(REDIS_KEY, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("RedisTemplate 缺失：写操作降级为静默跳过")
    void writesAreSkippedWithoutRedis() {
        ReflectionTestUtils.setField(service, "redisTemplate", (Object) null);

        service.saveWxRelation(USER_ID, "wx-app", "open-id");
        service.saveWxRelationMap(USER_ID, Map.of("wx-app", "open-id"));

        verifyNoInteractions(hashOperations);
    }

    @Test
    @DisplayName("RedisTemplate 缺失：读操作返回 null")
    void readsReturnNullWithoutRedis() {
        ReflectionTestUtils.setField(service, "redisTemplate", (Object) null);

        assertThat(service.getWxRelation(USER_ID, "wx-app")).isNull();
        assertThat(service.getWxRelationMap(USER_ID)).isNull();
        verifyNoInteractions(hashOperations);
    }
}
