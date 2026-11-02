package com.dusk.module.auth.cache;

import com.dusk.common.core.redis.RedisUtil;
import com.dusk.module.auth.dto.TenantFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisFeatureCache} 单元测试：覆盖 Redis 空缓存、已有缓存合并、
 * 应用/特性未命中以及汇总列表。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisFeatureCacheTest {

    private static final String FEATURE_KEY = "CRUX:AUTH:FEATURE";

    @Mock
    private RedisUtil<Object> redisUtil;

    private RedisFeatureCache cache;

    private static TenantFeature feature(String name, String defaultValue) {
        TenantFeature feature = new TenantFeature();
        feature.setName(name);
        feature.setDefaultValue(defaultValue);
        return feature;
    }

    private static Map<String, Map<String, TenantFeature>> cached(String app, TenantFeature... features) {
        Map<String, TenantFeature> appMap = new HashMap<>();
        for (TenantFeature feature : features) {
            appMap.put(feature.getName(), feature);
        }
        Map<String, Map<String, TenantFeature>> map = new HashMap<>();
        map.put(app, appMap);
        return map;
    }

    @BeforeEach
    void setUp() {
        cache = new RedisFeatureCache();
        ReflectionTestUtils.setField(cache, "redisUtil", redisUtil);
    }

    @Test
    @DisplayName("addDefaultFeature：Redis 为空时新建应用维度写入")
    void addWhenCacheEmpty() {
        when(redisUtil.getCache(FEATURE_KEY)).thenReturn(null);

        cache.addDefaultFeature("app", List.of(feature("f", "v")));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(redisUtil).setCache(eq(FEATURE_KEY), captor.capture());
        Map<String, Map<String, TenantFeature>> stored =
                (Map<String, Map<String, TenantFeature>>) captor.getValue();
        assertThat(stored).containsOnlyKeys("app");
        assertThat(stored.get("app")).containsOnlyKeys("f");
    }

    @Test
    @DisplayName("addDefaultFeature：保留其他应用已有特性")
    void addKeepsOtherApplications() {
        Map<String, Map<String, TenantFeature>> existing = cached("other", feature("keep", "1"));
        when(redisUtil.getCache(FEATURE_KEY)).thenReturn(existing);

        cache.addDefaultFeature("app", List.of(feature("f", "v")));

        assertThat(existing).containsOnlyKeys("other", "app");
    }

    @Test
    @DisplayName("getDefaultFeature：应用与特性都存在时返回特性")
    void getFeatureHit() {
        when(redisUtil.getCache(FEATURE_KEY)).thenReturn(cached("app", feature("f", "v")));

        assertThat(cache.getDefaultFeature("app", "f").getDefaultValue()).isEqualTo("v");
    }

    @Test
    @DisplayName("getDefaultFeature：应用不存在或特性不存在时返回 null")
    void getFeatureMiss() {
        when(redisUtil.getCache(FEATURE_KEY)).thenReturn(cached("app", feature("f", "v")));

        assertThat(cache.getDefaultFeature("unknown", "f")).isNull();
        assertThat(cache.getDefaultFeature("app", "missing")).isNull();
    }

    @Test
    @DisplayName("getDefaultFeatureList：缓存为空时返回空列表")
    void getFeatureListWhenCacheEmpty() {
        when(redisUtil.getCache(FEATURE_KEY)).thenReturn(null);

        assertThat(cache.getDefaultFeatureList()).isEmpty();
    }

    @Test
    @DisplayName("getDefaultFeatureList：汇总多个应用的特性")
    void getFeatureListMergesApplications() {
        Map<String, Map<String, TenantFeature>> existing = cached("a", feature("a1", "1"));
        existing.put("b", new HashMap<>(Map.of("b1", feature("b1", "2"))));
        when(redisUtil.getCache(FEATURE_KEY)).thenReturn(existing);

        assertThat(cache.getDefaultFeatureList())
                .extracting(TenantFeature::getName)
                .containsExactlyInAnyOrder("a1", "b1");
    }

    @Test
    @DisplayName("addDefaultFeature：空特性列表也可写入")
    void addEmptyFeatureList() {
        when(redisUtil.getCache(FEATURE_KEY)).thenReturn(null);

        cache.addDefaultFeature("app", List.of());

        verify(redisUtil).setCache(eq(FEATURE_KEY), any());
    }
}
