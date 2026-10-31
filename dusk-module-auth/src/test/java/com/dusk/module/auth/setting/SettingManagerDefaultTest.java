package com.dusk.module.auth.setting;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.entity.Setting;
import com.dusk.module.auth.repository.ISettingRepository;
import com.dusk.module.auth.service.ITenantService;
import com.dusk.module.auth.setting.config.MultiTenancyConfig;
import com.dusk.module.metadata.dto.SettingDefinition;
import com.dusk.module.metadata.enums.SettingScopes;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link SettingManagerDefault} 单元测试。
 *
 * <p>覆盖四级设置（应用/租户/厂站/用户）的默认值合并、继承与回退、
 * 写设置的增删改分支，以及 QueryDSL 条件构造（Specification）中按上下文生成条件的全部分支。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettingManagerDefaultTest {

    @Mock
    private ISettingsCache settingsCache;
    @Mock
    private ITenantService tenantService;
    @Mock
    private MultiTenancyConfig multiTenancyConfig;
    @Mock
    private ISettingRepository settingRepository;

    private TestableManager manager;

    private static SettingDefinition definition(String name, String defaultValue, boolean inherited,
                                                SettingScopes... scopes) {
        SettingDefinition definition = new SettingDefinition(name, defaultValue);
        definition.setScopes(scopes);
        definition.setInherited(inherited);
        return definition;
    }

    private static SettingInfo info(Long tenantId, Long stationId, Long userId, String name, String value) {
        return new SettingInfo(tenantId, stationId, userId, name, value);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static Setting settingEntity(Long tenantId, Long stationId, Long userId, String name, String value) {
        Setting setting = new Setting();
        setting.setTenantId(tenantId);
        setting.setStationId(stationId);
        setting.setUserId(userId);
        setting.setName(name);
        setting.setValue(value);
        return setting;
    }

    @BeforeEach
    void setUp() {
        manager = new TestableManager();
        ReflectionTestUtils.setField(manager, "settingsCache", settingsCache);
        ReflectionTestUtils.setField(manager, "tenantService", tenantService);
        ReflectionTestUtils.setField(manager, "multiTenancyConfig", multiTenancyConfig);
        ReflectionTestUtils.setField(manager, "settingRepository", settingRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        DataFilterContextHolder.clear();
        LoginUserIdContextHolder.clear();
    }

    private void cacheDefinition(SettingDefinition definition) {
        when(settingsCache.getSettingDefinition(definition.getName())).thenReturn(definition);
    }

    @Test
    @DisplayName("用户作用域命中用户设置时直接返回用户值")
    void userScopeReturnsUserValue() {
        cacheDefinition(definition("n", "def", true, SettingScopes.User));
        manager.context(1L, 2L, 3L);
        manager.putUser(info(1L, 2L, 3L, "n", "user-value"));

        assertThat(manager.getSettingValue("n")).isEqualTo("user-value");
    }

    // ------------------------------------------------------------------
    // 读设置：按作用域合并
    // ------------------------------------------------------------------

    @Test
    @DisplayName("用户作用域未命中且不继承时返回定义默认值")
    void userScopeNonInheritedReturnsDefault() {
        cacheDefinition(definition("n", "def", false, SettingScopes.User));
        manager.context(1L, 2L, 3L);

        assertThat(manager.getSettingValue("n")).isEqualTo("def");
    }

    @Test
    @DisplayName("用户作用域可继承时继续向厂站/租户/应用回退")
    void userScopeInheritedFallsThroughToStation() {
        cacheDefinition(definition("n", "def", true, SettingScopes.User, SettingScopes.Station));
        manager.context(1L, 2L, 3L);
        manager.putStation(info(1L, 2L, null, "n", "station-value"));

        assertThat(manager.getSettingValue("n")).isEqualTo("station-value");
    }

    @Test
    @DisplayName("用户作用域禁用回退时未命中返回 null")
    void userScopeWithoutFallbackReturnsNull() {
        cacheDefinition(definition("n", "def", true, SettingScopes.User));
        manager.context(1L, 2L, 3L);

        Object result = ReflectionTestUtils.invokeMethod(manager, "getSettingValueInternal",
                "n", 1L, 2L, 3L, false);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("厂站作用域命中厂站设置")
    void stationScopeReturnsStationValue() {
        cacheDefinition(definition("n", "def", true, SettingScopes.Station));
        manager.context(1L, 2L, null);
        manager.putStation(info(1L, 2L, null, "n", "station-value"));

        assertThat(manager.getSettingValue("n")).isEqualTo("station-value");
    }

    @Test
    @DisplayName("厂站作用域未命中且不继承时返回默认值")
    void stationScopeNonInheritedReturnsDefault() {
        cacheDefinition(definition("n", "def", false, SettingScopes.Station));
        manager.context(1L, 2L, null);

        assertThat(manager.getSettingValue("n")).isEqualTo("def");
    }

    @Test
    @DisplayName("厂站作用域禁用回退时未命中返回 null")
    void stationScopeWithoutFallbackReturnsNull() {
        cacheDefinition(definition("n", "def", true, SettingScopes.Station));
        manager.context(1L, 2L, 3L);

        assertThat(ReflectionTestUtils.<String>invokeMethod(manager, "getSettingValueInternal",
                "n", 1L, 2L, 3L, false)).isNull();
    }

    @Test
    @DisplayName("租户作用域命中租户设置")
    void tenantScopeReturnsTenantValue() {
        cacheDefinition(definition("n", "def", true, SettingScopes.Tenant));
        manager.context(1L, null, null);
        manager.putTenant(info(1L, null, null, "n", "tenant-value"));

        assertThat(manager.getSettingValueForTenant("n")).isEqualTo("tenant-value");
    }

    @Test
    @DisplayName("租户作用域未命中且不继承时返回默认值")
    void tenantScopeNonInheritedReturnsDefault() {
        cacheDefinition(definition("n", "def", false, SettingScopes.Tenant));
        manager.context(1L, null, null);

        assertThat(manager.getSettingValueForTenant("n")).isEqualTo("def");
    }

    @Test
    @DisplayName("租户作用域禁用回退时未命中返回 null")
    void tenantScopeWithoutFallbackReturnsNull() {
        cacheDefinition(definition("n", "def", true, SettingScopes.Tenant));
        manager.context(1L, null, null);

        assertThat(ReflectionTestUtils.<String>invokeMethod(manager, "getSettingValueInternal",
                "n", 1L, 2L, 3L, false)).isNull();
    }

    @Test
    @DisplayName("应用作用域命中应用设置")
    void applicationScopeReturnsApplicationValue() {
        when(multiTenancyConfig.isEnable()).thenReturn(true);
        cacheDefinition(definition("n", "def", true, SettingScopes.Application));
        manager.context(1L, null, null);
        manager.putApplication(info(null, null, null, "n", "app-value"));

        assertThat(manager.getSettingValueForApplication("n")).isEqualTo("app-value");
    }

    @Test
    @DisplayName("应用作用域未命中返回定义默认值")
    void applicationScopeReturnsDefaultWhenMissing() {
        when(multiTenancyConfig.isEnable()).thenReturn(true);
        cacheDefinition(definition("n", "def", true, SettingScopes.Application));

        assertThat(manager.getSettingValueForApplication("n")).isEqualTo("def");
    }

    @Test
    @DisplayName("应用作用域禁用回退时未命中返回 null")
    void applicationScopeWithoutFallbackReturnsNull() {
        when(multiTenancyConfig.isEnable()).thenReturn(true);
        cacheDefinition(definition("n", "def", true, SettingScopes.Application));

        assertThat(ReflectionTestUtils.<String>invokeMethod(manager, "getSettingValueInternal",
                "n", 1L, 2L, 3L, false)).isNull();
    }

    @Test
    @DisplayName("多租户关闭时应用级取值回退到租户设置")
    void applicationScopeUsesTenantWhenMultitenancyDisabled() {
        when(multiTenancyConfig.isEnable()).thenReturn(false);
        cacheDefinition(definition("n", "def", true, SettingScopes.Application));
        manager.context(1L, null, null);
        manager.putTenant(info(1L, null, null, "n", "tenant-value"));

        assertThat(manager.getSettingValueForApplication("n")).isEqualTo("tenant-value");
    }

    @Test
    @DisplayName("上下文为空时跳过用户/厂站/租户作用域并返回默认值")
    void skipsContextScopesWhenContextMissing() {
        cacheDefinition(definition("n", "def", true,
                SettingScopes.User, SettingScopes.Station, SettingScopes.Tenant, SettingScopes.Application));
        manager.context(null, null, null);

        assertThat(manager.getSettingValue("n")).isEqualTo("def");
    }

    @Test
    @DisplayName("定义只含应用作用域时即使上下文齐全也不会读取用户/厂站/租户值")
    void ignoresContextScopesWhenDefinitionDoesNotContainThem() {
        when(multiTenancyConfig.isEnable()).thenReturn(true);
        cacheDefinition(definition("n", "def", true, SettingScopes.Application));
        manager.context(1L, 2L, 3L);
        manager.putUser(info(1L, 2L, 3L, "n", "user-value"));
        manager.putStation(info(1L, 2L, null, "n", "station-value"));
        manager.putTenant(info(1L, null, null, "n", "tenant-value"));
        manager.putApplication(info(null, null, null, "n", "app-value"));

        assertThat(manager.getSettingValue("n")).isEqualTo("app-value");
    }

    @Test
    @DisplayName("getSettingValueForUser/Station 使用完整上下文")
    void stationAndUserGettersUseContext() {
        cacheDefinition(definition("n", "def", true, SettingScopes.User, SettingScopes.Station));
        manager.context(1L, 2L, 3L);
        manager.putUser(info(1L, 2L, 3L, "n", "user-value"));
        manager.putStation(info(1L, 2L, null, "n", "station-value"));

        assertThat(manager.getSettingValueForUser("n")).isEqualTo("user-value");
        assertThat(manager.getSettingValueForStation("n")).isEqualTo("station-value");
    }

    private SettingManagerDefault plainManager() {
        SettingManagerDefault plain = new SettingManagerDefault();
        ReflectionTestUtils.setField(plain, "settingsCache", settingsCache);
        ReflectionTestUtils.setField(plain, "tenantService", tenantService);
        ReflectionTestUtils.setField(plain, "multiTenancyConfig", multiTenancyConfig);
        ReflectionTestUtils.setField(plain, "settingRepository", settingRepository);
        return plain;
    }

    // ------------------------------------------------------------------
    // 读设置：父类基于 Repository 的 Map 构建
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getUserSettings 从 Repository 查询并映射为用户级 Map")
    void getUserSettingsFromRepository() {
        TenantContextHolder.setTenantId(1L);
        DataFilterContextHolder.setDataFilterId("2");
        LoginUserIdContextHolder.setUserId(3L);
        when(settingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(settingEntity(1L, 2L, 3L, "n", "v")));

        Map<String, SettingInfo> result = plainManager().getUserSettings();

        assertThat(result).containsKey("n");
        assertThat(result.get("n").getValue()).isEqualTo("v");
    }

    @Test
    @DisplayName("getStationSettings 从 Repository 查询并映射为厂站级 Map")
    void getStationSettingsFromRepository() {
        TenantContextHolder.setTenantId(1L);
        DataFilterContextHolder.setDataFilterId("2");
        when(settingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(settingEntity(1L, 2L, null, "n", "v")));

        assertThat(plainManager().getStationSettings()).containsKey("n");
    }

    @Test
    @DisplayName("getApplicationSettings 从 Repository 查询并映射为应用级 Map")
    void getApplicationSettingsFromRepository() {
        when(settingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(settingEntity(null, null, null, "n", "v")));

        assertThat(plainManager().getApplicationSettings()).containsKey("n");
    }

    @Test
    @DisplayName("getTenantSettings：无租户上下文直接返回空 Map")
    void getTenantSettingsWithoutTenantContext() {
        assertThat(plainManager().getTenantSettings()).isEmpty();
        verify(tenantService, never()).findById(anyLong());
    }

    @Test
    @DisplayName("getTenantSettings：多租户关闭且租户不存在时返回空 Map")
    void getTenantSettingsWhenTenantMissing() {
        TenantContextHolder.setTenantId(1L);
        when(multiTenancyConfig.isEnable()).thenReturn(false);
        when(tenantService.findById(1L)).thenReturn(Optional.empty());

        assertThat(plainManager().getTenantSettings()).isEmpty();
        verify(settingRepository, never()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("getTenantSettings：多租户关闭但租户存在时查询设置")
    void getTenantSettingsWhenTenantExists() {
        TenantContextHolder.setTenantId(1L);
        when(multiTenancyConfig.isEnable()).thenReturn(false);
        when(tenantService.findById(1L)).thenReturn(Optional.of(new com.dusk.module.auth.entity.Tenant()));
        when(settingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(settingEntity(1L, null, null, "n", "v")));

        assertThat(plainManager().getTenantSettings()).containsKey("n");
    }

    @Test
    @DisplayName("getTenantSettings：多租户开启时不校验租户是否存在")
    void getTenantSettingsWhenMultitenancyEnabled() {
        TenantContextHolder.setTenantId(1L);
        when(multiTenancyConfig.isEnable()).thenReturn(true);
        when(settingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(settingEntity(1L, null, null, "n", "v")));

        assertThat(plainManager().getTenantSettings()).containsKey("n");
        verify(tenantService, never()).findById(anyLong());
    }

    @Test
    @DisplayName("changeSettingForApplication：多租户开启时写入应用级设置")
    void changeApplicationWithMultitenancyEnabled() {
        when(multiTenancyConfig.isEnable()).thenReturn(true);
        cacheDefinition(definition("n", "def", false, SettingScopes.Application));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        manager.changeSettingForApplication("n", "v");

        ArgumentCaptor<Setting> captor = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isNull();
        assertThat(captor.getValue().getValue()).isEqualTo("v");
    }

    // ------------------------------------------------------------------
    // 写设置
    // ------------------------------------------------------------------

    @Test
    @DisplayName("changeSettingForApplication：多租户关闭时写入当前租户设置")
    void changeApplicationDelegatesToTenantWhenMultitenancyDisabled() {
        when(multiTenancyConfig.isEnable()).thenReturn(false);
        cacheDefinition(definition("n", "def", false, SettingScopes.Application));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        manager.context(9L, null, null);

        manager.changeSettingForApplication("n", "v");

        ArgumentCaptor<Setting> captor = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("changeSettingForTenant/Station/User 写入对应层级字段")
    void changeSettingsForEachScope() {
        cacheDefinition(definition("n", "def", false, SettingScopes.All));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        manager.context(1L, 2L, 3L);

        manager.changeSettingForTenant("n", "tenant");
        manager.changeSettingForStation("n", "station");
        manager.changeSettingForUser("n", "user");

        ArgumentCaptor<Setting> captor = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository, times(3)).save(captor.capture());
        List<Setting> saved = captor.getAllValues();
        assertThat(saved.get(0).getTenantId()).isEqualTo(1L);
        assertThat(saved.get(0).getStationId()).isNull();
        assertThat(saved.get(0).getUserId()).isNull();
        assertThat(saved.get(1).getStationId()).isEqualTo(2L);
        assertThat(saved.get(2).getUserId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("写入 null 值会转换为空字符串")
    void changeSettingConvertsNullToEmpty() {
        cacheDefinition(definition("n", "def", false, SettingScopes.Tenant));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        manager.context(1L, null, null);

        manager.changeSettingForTenant("n", null);

        ArgumentCaptor<Setting> captor = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository).save(captor.capture());
        assertThat(captor.getValue().getValue()).isEmpty();
    }

    @Test
    @DisplayName("值等于默认值且已有记录时删除旧设置")
    void deletesSettingWhenValueEqualsDefault() {
        cacheDefinition(definition("n", "def", false, SettingScopes.Tenant));
        Setting existing = settingEntity(1L, null, null, "n", "old");
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.of(existing));
        when(settingRepository.findAll(any(Specification.class))).thenReturn(List.of(existing));
        manager.context(1L, null, null);

        manager.changeSettingForTenant("n", "def");

        verify(settingRepository).deleteInBatch(List.of(existing));
        verify(settingRepository, never()).save(any(Setting.class));
    }

    @Test
    @DisplayName("值等于默认值且无记录时不写入")
    void skipsWriteWhenValueEqualsDefaultAndNoRecord() {
        cacheDefinition(definition("n", "def", false, SettingScopes.Tenant));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        manager.context(1L, null, null);

        manager.changeSettingForTenant("n", "def");

        verify(settingRepository, never()).save(any(Setting.class));
        verify(settingRepository, never()).deleteInBatch(any());
    }

    @Test
    @DisplayName("已有记录且值相同则不更新")
    void skipsUpdateWhenValueUnchanged() {
        cacheDefinition(definition("n", "def", false, SettingScopes.Tenant));
        Setting existing = settingEntity(1L, null, null, "n", "same");
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.of(existing));
        manager.context(1L, null, null);

        manager.changeSettingForTenant("n", "same");

        verify(settingRepository, never()).save(any(Setting.class));
    }

    @Test
    @DisplayName("已有记录且值不同时更新记录")
    void updatesExistingSetting() {
        cacheDefinition(definition("n", "def", false, SettingScopes.Tenant));
        Setting existing = settingEntity(1L, null, null, "n", "old");
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.of(existing));
        manager.context(1L, null, null);

        manager.changeSettingForTenant("n", "new");

        verify(settingRepository).save(existing);
        assertThat(existing.getValue()).isEqualTo("new");
    }

    @Test
    @DisplayName("update：记录不存在时不保存")
    void updateDoesNothingWhenRecordMissing() {
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        manager.update(info(1L, null, null, "n", "v"));

        verify(settingRepository, never()).save(any(Setting.class));
    }

    @Test
    @DisplayName("create：SettingInfo 转换为实体后保存")
    void createSavesEntity() {
        manager.create(info(1L, 2L, 3L, "n", "v"));

        ArgumentCaptor<Setting> captor = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("n");
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getStationId()).isEqualTo(2L);
        assertThat(captor.getValue().getUserId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("delete：按 SettingInfo 条件查询后批量删除")
    void deleteRemovesMatchedSettings() {
        Setting existing = settingEntity(1L, null, null, "n", "v");
        when(settingRepository.findAll(any(Specification.class))).thenReturn(List.of(existing));

        manager.delete(info(1L, null, null, "n", "v"));

        verify(settingRepository).deleteInBatch(List.of(existing));
    }

    @Test
    @DisplayName("继承设置：应用值覆盖定义默认值（多租户开启）")
    void inheritedDefaultUsesApplicationValue() {
        when(multiTenancyConfig.isEnable()).thenReturn(true);
        cacheDefinition(definition("n", "def", true, SettingScopes.Tenant));
        manager.context(1L, null, null);
        manager.putApplication(info(null, null, null, "n", "app-value"));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        manager.changeSettingForTenant("n", "app-value");

        verify(settingRepository, never()).save(any(Setting.class));
    }

    // ------------------------------------------------------------------
    // 继承默认值：写设置时的默认值覆盖链
    // ------------------------------------------------------------------

    @Test
    @DisplayName("继承设置：应用值缺失时保留定义默认值")
    void inheritedDefaultKeepsDefinitionValueWhenApplicationMissing() {
        when(multiTenancyConfig.isEnable()).thenReturn(true);
        cacheDefinition(definition("n", "def", true, SettingScopes.Tenant));
        manager.context(1L, null, null);
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        manager.changeSettingForTenant("n", "def");

        verify(settingRepository, never()).save(any(Setting.class));
    }

    @Test
    @DisplayName("继承设置：租户值覆盖应用默认值")
    void inheritedDefaultUsesTenantValue() {
        cacheDefinition(definition("n", "def", true, SettingScopes.Station));
        manager.context(1L, 2L, null);
        manager.putTenant(info(1L, null, null, "n", "tenant-value"));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        manager.changeSettingForStation("n", "tenant-value");

        verify(settingRepository, never()).save(any(Setting.class));
    }

    @Test
    @DisplayName("继承设置：租户值缺失时保留定义默认值")
    void inheritedDefaultKeepsDefinitionValueWhenTenantMissing() {
        cacheDefinition(definition("n", "def", true, SettingScopes.Station));
        manager.context(1L, 2L, null);
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        manager.changeSettingForStation("n", "def");

        verify(settingRepository, never()).save(any(Setting.class));
    }

    @Test
    @DisplayName("继承设置：厂站值覆盖租户默认值")
    void inheritedDefaultUsesStationValue() {
        cacheDefinition(definition("n", "def", true, SettingScopes.User));
        manager.context(1L, 2L, 3L);
        manager.putStation(info(1L, 2L, null, "n", "station-value"));
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        manager.changeSettingForUser("n", "station-value");

        verify(settingRepository, never()).save(any(Setting.class));
    }

    @Test
    @DisplayName("继承设置：厂站值缺失时保留定义默认值")
    void inheritedDefaultKeepsDefinitionValueWhenStationMissing() {
        cacheDefinition(definition("n", "def", true, SettingScopes.User));
        manager.context(1L, 2L, 3L);
        when(settingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        manager.changeSettingForUser("n", "def");

        verify(settingRepository, never()).save(any(Setting.class));
    }

    @Test
    @DisplayName("createQuery：按上下文生成 tenant/station/user 的等值或 isNull 条件")
    @SuppressWarnings("unchecked")
    void createQueryCoversAllConditionBranches() {
        SettingManagerDefault plain = plainManager();
        when(settingRepository.findAll(any(Specification.class))).thenReturn(List.of());

        TenantContextHolder.setTenantId(1L);
        DataFilterContextHolder.setDataFilterId("2");
        LoginUserIdContextHolder.setUserId(3L);
        plain.getUserSettings();

        TenantContextHolder.clear();
        DataFilterContextHolder.clear();
        LoginUserIdContextHolder.clear();
        plain.getUserSettings();

        ArgumentCaptor<Specification<Setting>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(settingRepository, times(2)).findAll(captor.capture());

        Root<Setting> root = org.mockito.Mockito.mock(Root.class);
        Path<Object> path = org.mockito.Mockito.mock(Path.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder builder = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);

        doReturn(path).when(root).get(anyString());
        when(builder.equal(any(jakarta.persistence.criteria.Expression.class), any(Object.class))).thenReturn(predicate);
        when(builder.isNull(any())).thenReturn(predicate);
        when(builder.and(any(Predicate[].class))).thenReturn(predicate);

        for (Specification<Setting> specification : captor.getAllValues()) {
            assertThat(specification.toPredicate(root, query, builder)).isSameAs(predicate);
        }
        verify(builder, times(3)).equal(any(jakarta.persistence.criteria.Expression.class), any(Object.class));
        verify(builder, times(3)).isNull(any());
    }

    // ------------------------------------------------------------------
    // Specification 条件构造
    // ------------------------------------------------------------------

    /**
     * 可直接注入四级设置 Map 的测试替身，避免每条用例都走 Repository。
     */
    private static final class TestableManager extends SettingManagerDefault {
        private Long tenantId;
        private Long stationId;
        private Long userId;
        private Map<String, SettingInfo> userSettings = new HashMap<>();
        private Map<String, SettingInfo> stationSettings = new HashMap<>();
        private Map<String, SettingInfo> tenantSettings = new HashMap<>();
        private Map<String, SettingInfo> applicationSettings = new HashMap<>();

        void context(Long tenantId, Long stationId, Long userId) {
            this.tenantId = tenantId;
            this.stationId = stationId;
            this.userId = userId;
        }

        void putUser(SettingInfo settingInfo) {
            userSettings.put(settingInfo.getName(), settingInfo);
        }

        void putStation(SettingInfo settingInfo) {
            stationSettings.put(settingInfo.getName(), settingInfo);
        }

        void putTenant(SettingInfo settingInfo) {
            tenantSettings.put(settingInfo.getName(), settingInfo);
        }

        void putApplication(SettingInfo settingInfo) {
            applicationSettings.put(settingInfo.getName(), settingInfo);
        }

        @Override
        protected Long getTenantId() {
            return tenantId;
        }

        @Override
        protected Long getStationId() {
            return stationId;
        }

        @Override
        protected Long getCurrentUserId() {
            return userId;
        }

        @Override
        protected Map<String, SettingInfo> getUserSettings() {
            return userSettings;
        }

        @Override
        protected Map<String, SettingInfo> getStationSettings() {
            return stationSettings;
        }

        @Override
        protected Map<String, SettingInfo> getTenantSettings() {
            return tenantSettings;
        }

        @Override
        protected Map<String, SettingInfo> getApplicationSettings() {
            return applicationSettings;
        }
    }
}
