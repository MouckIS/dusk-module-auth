package com.dusk.module.auth.setting;

import com.dusk.common.core.redis.RedisUtil;
import com.dusk.module.metadata.dto.SettingDefinition;
import com.dusk.module.metadata.enums.SettingScopes;
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
 * {@link RedisSettingsCache} 单元测试：覆盖 Redis 空值/异常兜底、应用维度合并
 * 以及按作用域过滤的分支。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisSettingsCacheTest {

    private static final String SETTING_DEFINITION_KEY = "CRUX:SYS:SETTING:DEFINITION:MAPS";

    @Mock
    private RedisUtil<Object> redisUtil;

    private RedisSettingsCache cache;

    private static SettingDefinition definition(String name, SettingScopes... scopes) {
        SettingDefinition definition = new SettingDefinition(name, name + "-default");
        definition.setScopes(scopes);
        return definition;
    }

    @BeforeEach
    void setUp() {
        cache = new RedisSettingsCache();
        ReflectionTestUtils.setField(cache, "redisUtil", redisUtil);
    }

    @Test
    @DisplayName("addSettingDefinitions：缓存为空时新建应用维度并写回 Redis")
    void addSettingDefinitionsWhenCacheEmpty() {
        when(redisUtil.getCache(SETTING_DEFINITION_KEY)).thenReturn(null);

        cache.addSettingDefinitions("app", List.of(definition("a", SettingScopes.Application)));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(redisUtil).setCache(eq(SETTING_DEFINITION_KEY), captor.capture());
        Map<String, Map<String, SettingDefinition>> stored =
                (Map<String, Map<String, SettingDefinition>>) captor.getValue();
        assertThat(stored).containsOnlyKeys("app");
        assertThat(stored.get("app")).containsOnlyKeys("a");
    }

    @Test
    @DisplayName("addSettingDefinitions：保留其他应用已缓存的定义")
    void addSettingDefinitionsKeepsOtherApplications() {
        Map<String, Map<String, SettingDefinition>> existing = new HashMap<>();
        existing.put("other", new HashMap<>(Map.of("keep", definition("keep", SettingScopes.Tenant))));
        when(redisUtil.getCache(SETTING_DEFINITION_KEY)).thenReturn(existing);

        cache.addSettingDefinitions("app", List.of(definition("a", SettingScopes.Application)));

        assertThat(existing).containsOnlyKeys("other", "app");
    }

    @Test
    @DisplayName("addSettingDefinitions：Redis 读取异常时按空缓存处理")
    void addSettingDefinitionsWhenRedisReadFails() {
        when(redisUtil.getCache(SETTING_DEFINITION_KEY)).thenThrow(new IllegalStateException("redis down"));

        cache.addSettingDefinitions("app", List.of(definition("a", SettingScopes.Application)));

        verify(redisUtil).setCache(eq(SETTING_DEFINITION_KEY), any());
    }

    @Test
    @DisplayName("getAllSettingDefinitions：缓存值不是 Map 时忽略并返回空集合")
    void getAllSettingDefinitionsIgnoresUnexpectedCacheType() {
        when(redisUtil.getCache(SETTING_DEFINITION_KEY)).thenReturn("not-a-map");

        assertThat(cache.getAllSettingDefinitions()).isEmpty();
    }

    @Test
    @DisplayName("getAllSettingDefinitions：缓存命中时打平所有应用")
    void getAllSettingDefinitionsFromCache() {
        Map<String, Map<String, SettingDefinition>> cached = new HashMap<>();
        cached.put("app-a", new HashMap<>(Map.of("a", definition("a", SettingScopes.Application))));
        cached.put("app-b", new HashMap<>(Map.of("b", definition("b", SettingScopes.Tenant))));
        when(redisUtil.getCache(SETTING_DEFINITION_KEY)).thenReturn(cached);

        assertThat(cache.getAllSettingDefinitions()).containsOnlyKeys("a", "b");
    }

    @Test
    @DisplayName("按作用域过滤：包含 All 作用域以及不匹配的定义")
    void filtersByScope() {
        Map<String, Map<String, SettingDefinition>> cached = new HashMap<>();
        cached.put("app", new HashMap<>(Map.of(
                "application", definition("application", SettingScopes.Application),
                "tenant", definition("tenant", SettingScopes.Tenant),
                "station", definition("station", SettingScopes.Station),
                "user", definition("user", SettingScopes.User),
                "all", definition("all", SettingScopes.All))));
        when(redisUtil.getCache(SETTING_DEFINITION_KEY)).thenReturn(cached);

        assertThat(cache.getAllApplicationSettingDefinitions()).containsOnlyKeys("application", "all");
        assertThat(cache.getAllTenantSettingDefinitions()).containsOnlyKeys("tenant", "all");
        assertThat(cache.getAllStationSettingDefinitions()).containsOnlyKeys("station", "all");
        assertThat(cache.getAllUserSettingDefinitions()).containsOnlyKeys("user", "all");
    }

    @Test
    @DisplayName("getSettingDefinition：命中返回定义，未命中返回 null")
    void getSettingDefinition() {
        Map<String, Map<String, SettingDefinition>> cached = new HashMap<>();
        cached.put("app", new HashMap<>(Map.of("known", definition("known", SettingScopes.Tenant))));
        when(redisUtil.getCache(SETTING_DEFINITION_KEY)).thenReturn(cached);

        assertThat(cache.getSettingDefinition("known")).isNotNull();
        assertThat(cache.getSettingDefinition("missing")).isNull();
    }
}
