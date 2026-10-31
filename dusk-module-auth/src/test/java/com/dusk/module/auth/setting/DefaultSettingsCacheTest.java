package com.dusk.module.auth.setting;

import com.dusk.module.metadata.dto.SettingDefinition;
import com.dusk.module.metadata.enums.SettingScopes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DefaultSettingsCache} 单元测试：覆盖应用级设置定义的内存缓存、
 * 按作用域过滤（含 {@link SettingScopes#All}）以及同名定义覆盖。
 */
class DefaultSettingsCacheTest {

    private DefaultSettingsCache cache;

    private static SettingDefinition definition(String name, SettingScopes... scopes) {
        SettingDefinition definition = new SettingDefinition(name, name + "-default");
        definition.setScopes(scopes);
        return definition;
    }

    @BeforeEach
    void setUp() {
        cache = new DefaultSettingsCache();
        Map<String, Map<String, SettingDefinition>> settingsMap =
                (Map<String, Map<String, SettingDefinition>>) ReflectionTestUtils.getField(cache, "settingsMap");
        settingsMap.clear();
    }

    @Test
    @DisplayName("多个应用的设置定义会被打平合并")
    void addSettingDefinitionsMergesApplications() {
        cache.addSettingDefinitions("app-a", List.of(
                definition("a1", SettingScopes.Application),
                definition("a2", SettingScopes.Tenant)));
        cache.addSettingDefinitions("app-b", List.of(
                definition("b1", SettingScopes.User)));

        Map<String, SettingDefinition> all = cache.getAllSettingDefinitions();

        assertThat(all).containsOnlyKeys("a1", "a2", "b1");
    }

    @Test
    @DisplayName("按作用域过滤：Application/Tenant/Station/User 只返回匹配项")
    void filtersByScope() {
        cache.addSettingDefinitions("app", List.of(
                definition("application", SettingScopes.Application),
                definition("tenant", SettingScopes.Tenant),
                definition("station", SettingScopes.Station),
                definition("user", SettingScopes.User),
                definition("all", SettingScopes.All)));

        assertThat(cache.getAllApplicationSettingDefinitions())
                .containsOnlyKeys("application", "all");
        assertThat(cache.getAllTenantSettingDefinitions())
                .containsOnlyKeys("tenant", "all");
        assertThat(cache.getAllStationSettingDefinitions())
                .containsOnlyKeys("station", "all");
        assertThat(cache.getAllUserSettingDefinitions())
                .containsOnlyKeys("user", "all");
    }

    @Test
    @DisplayName("同一应用重复注册会整体替换旧定义")
    void addSettingDefinitionsReplacesApplicationEntries() {
        cache.addSettingDefinitions("app", List.of(definition("old", SettingScopes.Application)));
        cache.addSettingDefinitions("app", List.of(definition("new", SettingScopes.Application)));

        assertThat(cache.getAllSettingDefinitions()).containsOnlyKeys("new");
        assertThat(cache.getSettingDefinition("old")).isNull();
    }

    @Test
    @DisplayName("getSettingDefinition：命中返回定义，未命中返回 null")
    void getSettingDefinition() {
        cache.addSettingDefinitions("app", List.of(definition("known", SettingScopes.Tenant)));

        assertThat(cache.getSettingDefinition("known").getName()).isEqualTo("known");
        assertThat(cache.getSettingDefinition("unknown")).isNull();
    }

    @Test
    @DisplayName("空定义列表不产生任何设置项")
    void addEmptyDefinitions() {
        cache.addSettingDefinitions("app", List.of());

        assertThat(cache.getAllSettingDefinitions()).isEmpty();
    }
}
