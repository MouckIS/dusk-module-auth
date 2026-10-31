package com.dusk.module.auth.setting;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.entity.Setting;
import com.dusk.module.auth.repository.ISettingRepository;
import com.dusk.module.auth.service.ITenantService;
import com.dusk.module.auth.setting.config.MultiTenancyConfig;
import com.dusk.module.metadata.dto.SettingDefinition;
import com.dusk.module.metadata.enums.SettingScopes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link SettingManagerCache} 单元测试：覆盖四级设置缓存的命中/未命中回源、
 * 缓存类型异常的兜底，以及写设置后主动失效 Redis 缓存的分支。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettingManagerCacheTest {

    private static final String APPLICATION_KEY = "CRUX:SETTINGS:APPLICATION";
    private static final String TENANT_KEY_PREFIX = "CRUX:SETTINGS:TENANT:";
    private static final String STATION_KEY_PREFIX = "CRUX:SETTINGS:STATION:";
    private static final String USER_KEY_PREFIX = "CRUX:SETTINGS:USER:";

    @Mock
    private RedisUtil<Object> redisUtil;
    @Mock
    private ISettingsCache settingsCache;
    @Mock
    private ITenantService tenantService;
    @Mock
    private MultiTenancyConfig multiTenancyConfig;
    @Mock
    private ISettingRepository settingRepository;

    private SettingManagerCache cache;

    private static SettingDefinition definition(String name) {
        SettingDefinition definition = new SettingDefinition(name, "def");
        definition.setScopes(new SettingScopes[]{SettingScopes.All});
        definition.setInherited(false);
        return definition;
    }

    private static Setting settingEntity(String name, String value) {
        Setting setting = new Setting();
        setting.setName(name);
        setting.setValue(value);
        return setting;
    }

    private static Map<String, SettingInfo> cachedMap(String name, String value) {
        Map<String, SettingInfo> map = new HashMap<>();
        map.put(name, new SettingInfo(null, null, null, name, value));
        return map;
    }

    @BeforeEach
    void setUp() {
        cache = new SettingManagerCache();
        ReflectionTestUtils.setField(cache, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(cache, "settingsCache", settingsCache);
        ReflectionTestUtils.setField(cache, "tenantService", tenantService);
        ReflectionTestUtils.setField(cache, "multiTenancyConfig", multiTenancyConfig);
        ReflectionTestUtils.setField(cache, "settingRepository", settingRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        DataFilterContextHolder.clear();
        LoginUserIdContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // 读缓存
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getUserSettings：缓存命中时不再访问数据库")
    void getUserSettingsFromCache() {
        LoginUserIdContextHolder.setUserId(3L);
        Map<String, SettingInfo> cached = cachedMap("n", "user-cache");
        when(redisUtil.getCache(USER_KEY_PREFIX + 3L)).thenReturn(cached);

        assertThat(cache.getUserSettings()).isSameAs(cached);
        verify(settingRepository, never()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("getUserSettings：缓存未命中时回源并写回缓存")
    void getUserSettingsFallsBackToDatabase() {
        TenantContextHolder.setTenantId(1L);
        DataFilterContextHolder.setDataFilterId("2");
        LoginUserIdContextHolder.setUserId(3L);
        when(redisUtil.getCache(USER_KEY_PREFIX + 3L)).thenReturn(null);
        when(settingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(settingEntity("n", "user-db")));

        Map<String, SettingInfo> result = cache.getUserSettings();

        assertThat(result.get("n").getValue()).isEqualTo("user-db");
        verify(redisUtil).setCache(eq(USER_KEY_PREFIX + 3L), eq(result));
    }

    @Test
    @DisplayName("getStationSettings：缓存命中时直接返回")
    void getStationSettingsFromCache() {
        DataFilterContextHolder.setDataFilterId("2");
        Map<String, SettingInfo> cached = cachedMap("n", "station-cache");
        when(redisUtil.getCache(STATION_KEY_PREFIX + 2L)).thenReturn(cached);

        assertThat(cache.getStationSettings()).isSameAs(cached);
    }

    @Test
    @DisplayName("getStationSettings：缓存未命中时回源并写回缓存")
    void getStationSettingsFallsBackToDatabase() {
        TenantContextHolder.setTenantId(1L);
        DataFilterContextHolder.setDataFilterId("2");
        when(redisUtil.getCache(STATION_KEY_PREFIX + 2L)).thenReturn(null);
        when(settingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(settingEntity("n", "station-db")));

        Map<String, SettingInfo> result = cache.getStationSettings();

        assertThat(result.get("n").getValue()).isEqualTo("station-db");
        verify(redisUtil).setCache(eq(STATION_KEY_PREFIX + 2L), eq(result));
    }

    @Test
    @DisplayName("getTenantSettings：缓存命中时直接返回")
    void getTenantSettingsFromCache() {
        TenantContextHolder.setTenantId(1L);
        Map<String, SettingInfo> cached = cachedMap("n", "tenant-cache");
        when(redisUtil.getCache(TENANT_KEY_PREFIX + 1L)).thenReturn(cached);

        assertThat(cache.getTenantSettings()).isSameAs(cached);
    }

    @Test
    @DisplayName("getTenantSettings：缓存未命中时回源并写回缓存")
    void getTenantSettingsFallsBackToDatabase() {
        TenantContextHolder.setTenantId(1L);
        when(redisUtil.getCache(TENANT_KEY_PREFIX + 1L)).thenReturn(null);
        when(multiTenancyConfig.isEnable()).thenReturn(true);
        when(settingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(settingEntity("n", "tenant-db")));

        Map<String, SettingInfo> result = cache.getTenantSettings();

        assertThat(result.get("n").getValue()).isEqualTo("tenant-db");
        verify(redisUtil).setCache(eq(TENANT_KEY_PREFIX + 1L), eq(result));
    }

    @Test
    @DisplayName("getApplicationSettings：缓存命中时直接返回")
    void getApplicationSettingsFromCache() {
        Map<String, SettingInfo> cached = cachedMap("n", "app-cache");
        when(redisUtil.getCache(APPLICATION_KEY)).thenReturn(cached);

        assertThat(cache.getApplicationSettings()).isSameAs(cached);
    }

    @Test
    @DisplayName("getApplicationSettings：缓存未命中时回源并写回缓存")
    void getApplicationSettingsFallsBackToDatabase() {
        when(redisUtil.getCache(APPLICATION_KEY)).thenReturn(null);
        when(settingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(settingEntity("n", "app-db")));

        Map<String, SettingInfo> result = cache.getApplicationSettings();

        assertThat(result.get("n").getValue()).isEqualTo("app-db");
        verify(redisUtil).setCache(eq(APPLICATION_KEY), eq(result));
    }

    @Test
    @DisplayName("Redis 读取抛异常时回源数据库")
    void redisReadFailureFallsBackToDatabase() {
        LoginUserIdContextHolder.setUserId(3L);
        when(redisUtil.getCache(USER_KEY_PREFIX + 3L)).thenThrow(new IllegalStateException("redis down"));
        when(settingRepository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThat(cache.getUserSettings()).isEmpty();
        verify(settingRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("缓存值类型不是 Map 时忽略并回源数据库")
    void unexpectedCacheTypeFallsBackToDatabase() {
        LoginUserIdContextHolder.setUserId(3L);
        when(redisUtil.getCache(USER_KEY_PREFIX + 3L)).thenReturn("not-a-map");
        when(settingRepository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThat(cache.getUserSettings()).isEmpty();
        verify(settingRepository).findAll(any(Specification.class));
    }

    // ------------------------------------------------------------------
    // 写设置后失效缓存
    // ------------------------------------------------------------------

    @Test
    @DisplayName("changeSettingForApplication：多租户开启时失效应用缓存")
    void changeApplicationInvalidatesApplicationCache() {
        when(multiTenancyConfig.isEnable()).thenReturn(true);
        when(settingsCache.getSettingDefinition("n")).thenReturn(definition("n"));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        cache.changeSettingForApplication("n", "v");

        verify(redisUtil).setExpire(APPLICATION_KEY, -1);
    }

    @Test
    @DisplayName("changeSettingForApplication：多租户关闭时失效租户与应用缓存")
    void changeApplicationInvalidatesTenantCacheWhenMultitenancyDisabled() {
        when(multiTenancyConfig.isEnable()).thenReturn(false);
        when(settingsCache.getSettingDefinition("n")).thenReturn(definition("n"));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        TenantContextHolder.setTenantId(1L);

        cache.changeSettingForApplication("n", "v");

        verify(redisUtil).setExpire(TENANT_KEY_PREFIX + 1L, -1);
        verify(redisUtil).setExpire(APPLICATION_KEY, -1);
    }

    @Test
    @DisplayName("changeSettingForTenant/Station/User 分别失效对应层级缓存")
    void changeSettingsInvalidateScopeCaches() {
        when(settingsCache.getSettingDefinition("n")).thenReturn(definition("n"));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        TenantContextHolder.setTenantId(1L);
        DataFilterContextHolder.setDataFilterId("2");
        LoginUserIdContextHolder.setUserId(3L);

        cache.changeSettingForTenant("n", "tenant");
        cache.changeSettingForStation("n", "station");
        cache.changeSettingForUser("n", "user");

        verify(redisUtil).setExpire(TENANT_KEY_PREFIX + 1L, -1);
        verify(redisUtil).setExpire(STATION_KEY_PREFIX + 2L, -1);
        verify(redisUtil).setExpire(USER_KEY_PREFIX + 3L, -1);
    }
}
