package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.common.core.dto.NameValueDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.dto.setting.SettingDto;
import com.dusk.module.auth.dto.setting.UpdateSettingInput;
import com.dusk.module.auth.entity.Setting;
import com.dusk.module.auth.repository.ISettingRepository;
import com.dusk.module.auth.setting.ISettingManager;
import com.dusk.module.auth.setting.ISettingsCache;
import com.dusk.module.ddm.dto.SettingDefinition;
import com.dusk.module.ddm.dto.ui.FileInput;
import com.dusk.module.ddm.dto.ui.InputType;
import com.dusk.module.ddm.enums.SettingAccessLevel;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SettingServiceImpl} 单元测试，目标：覆盖 service.impl 门禁包的主机/租户/厂站/用户
 * 四级设置注册表的读写逻辑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettingServiceImplTest {

    @Mock
    private ISettingRepository repository;
    @Mock
    private ISettingsCache settingsCache;
    @Mock
    private ISettingManager settingManager;

    private SettingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SettingServiceImpl();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "settingsCache", settingsCache);
        ReflectionTestUtils.setField(service, "settingManager", settingManager);
    }

    @AfterEach
    void tearDown() {
        LoginUserIdContextHolder.clear();
        DataFilterContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static SettingDefinition definition(String name, SettingAccessLevel level) {
        SettingDefinition d = new SettingDefinition(name, name + " 默认值");
        d.setName(name);
        d.setDisplayName(name + "-display");
        d.setAccessLevel(level);
        return d;
    }

    private static SettingDefinition fileDefinition(String name, SettingAccessLevel level) {
        SettingDefinition d = definition(name, level);
        d.setInputType(new FileInput());
        return d;
    }

    private static Map<String, SettingDefinition> mapOf(SettingDefinition... definitions) {
        Map<String, SettingDefinition> map = new LinkedHashMap<>();
        for (SettingDefinition d : definitions) {
            map.put(d.getName(), d);
        }
        return map;
    }

    private static UpdateSettingInput inputOf(String name, String value) {
        UpdateSettingInput input = new UpdateSettingInput();
        input.setNameValues(new ArrayList<>(List.of(new NameValueDto<>(name, value))));
        return input;
    }

    // ------------------------------------------------------------------
    // 四级设置读取
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getApplicationSettings：映射定义并回填应用级取值")
    void getApplicationSettings() {
        when(settingsCache.getAllApplicationSettingDefinitions())
                .thenReturn(mapOf(definition("A", SettingAccessLevel.Public)));
        when(settingManager.getSettingValueForApplication("A")).thenReturn("v");

        List<SettingDto> result = service.getApplicationSettings();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("A");
        assertThat(result.getFirst().getValue()).isEqualTo("v");
    }

    @Test
    @DisplayName("getTenantSettings：映射定义并回填租户级取值")
    void getTenantSettings() {
        when(settingsCache.getAllTenantSettingDefinitions())
                .thenReturn(mapOf(definition("T", SettingAccessLevel.Login)));
        when(settingManager.getSettingValueForTenant("T")).thenReturn("tv");

        assertThat(service.getTenantSettings().getFirst().getValue()).isEqualTo("tv");
    }

    @Test
    @DisplayName("getStationSettings：映射定义并回填厂站级取值")
    void getStationSettings() {
        when(settingsCache.getAllStationSettingDefinitions())
                .thenReturn(mapOf(definition("S", SettingAccessLevel.Login)));
        when(settingManager.getSettingValueForStation("S")).thenReturn("sv");

        assertThat(service.getStationSettings().getFirst().getValue()).isEqualTo("sv");
    }

    @Test
    @DisplayName("getUserSettings：映射定义并回填用户级取值")
    void getUserSettings() {
        when(settingsCache.getAllUserSettingDefinitions())
                .thenReturn(mapOf(definition("U", SettingAccessLevel.Login)));
        when(settingManager.getSettingValueForUser("U")).thenReturn("uv");

        assertThat(service.getUserSettings().getFirst().getValue()).isEqualTo("uv");
    }

    @Test
    @DisplayName("getApplicationSettings：无定义返回空列表")
    void getApplicationSettings_empty() {
        when(settingsCache.getAllApplicationSettingDefinitions()).thenReturn(new LinkedHashMap<>());

        assertThat(service.getApplicationSettings()).isEmpty();
    }

    // ------------------------------------------------------------------
    // 初始化设置
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getAllSettingsForInit：未登录时只返回 Public 级设置")
    void getAllSettingsForInit_anonymous() {
        when(settingsCache.getAllSettingDefinitions()).thenReturn(mapOf(
                definition("pub", SettingAccessLevel.Public),
                definition("host", SettingAccessLevel.HostOnly)));
        when(settingManager.getSettingValueForUser("pub")).thenReturn("pv");

        Map<String, Map<String, String>> result = service.getAllSettingsForInit();

        assertThat(result).containsOnlyKeys("pub");
        assertThat(result.get("pub")).containsEntry("value", "pv");
    }

    @Test
    @DisplayName("getAllSettingsForInit：已登录时返回 Public 与 Login 级设置")
    void getAllSettingsForInit_loggedIn() {
        LoginUserIdContextHolder.setUserId(1L);
        when(settingsCache.getAllSettingDefinitions()).thenReturn(mapOf(
                definition("pub", SettingAccessLevel.Public),
                definition("login", SettingAccessLevel.Login),
                definition("host", SettingAccessLevel.HostOnly)));
        when(settingManager.getSettingValueForUser(anyString())).thenReturn("v");

        Map<String, Map<String, String>> result = service.getAllSettingsForInit();

        assertThat(result).containsOnlyKeys("pub", "login");
    }

    @Test
    @DisplayName("getAllSettingsForInit：文件类型设置值为空时不进入文件清单")
    void getAllSettingsForInit_fileInputBlankValue() {
        when(settingsCache.getAllSettingDefinitions())
                .thenReturn(mapOf(fileDefinition("file", SettingAccessLevel.Public)));
        when(settingManager.getSettingValueForUser("file")).thenReturn("   ");

        Map<String, Map<String, String>> result = service.getAllSettingsForInit();

        assertThat(result.get("file")).containsEntry("value", "   ");
    }

    @Test
    @DisplayName("getAllSettingsForInit：文件类型设置值有效时收集文件 id")
    void getAllSettingsForInit_fileInputWithValue() {
        when(settingsCache.getAllSettingDefinitions())
                .thenReturn(mapOf(fileDefinition("file", SettingAccessLevel.Public)));
        when(settingManager.getSettingValueForUser("file")).thenReturn("1024");

        Map<String, Map<String, String>> result = service.getAllSettingsForInit();

        assertThat(result.get("file")).containsEntry("value", "1024");
    }

    // ------------------------------------------------------------------
    // 四级设置写入
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updateApplicationSettings：值变化时写入应用级设置")
    void updateApplicationSettings() {
        when(settingsCache.getAllApplicationSettingDefinitions())
                .thenReturn(mapOf(definition("A", SettingAccessLevel.Public)));
        when(settingManager.getSettingValueForApplication("A")).thenReturn("old");

        service.updateApplicationSettings(inputOf("A", "new"));

        verify(settingManager).changeSettingForApplication("A", "new");
    }

    @Test
    @DisplayName("updateApplicationSettings：值未变化时不写入")
    void updateApplicationSettings_unchanged() {
        when(settingsCache.getAllApplicationSettingDefinitions())
                .thenReturn(mapOf(definition("A", SettingAccessLevel.Public)));
        when(settingManager.getSettingValueForApplication("A")).thenReturn("same");

        service.updateApplicationSettings(inputOf("A", "same"));

        verify(settingManager, never()).changeSettingForApplication(anyString(), anyString());
    }

    @Test
    @DisplayName("updateApplicationSettings：名称不匹配时不写入")
    void updateApplicationSettings_nameMismatch() {
        when(settingsCache.getAllApplicationSettingDefinitions())
                .thenReturn(mapOf(definition("A", SettingAccessLevel.Public)));

        service.updateApplicationSettings(inputOf("other", "new"));

        verify(settingManager, never()).changeSettingForApplication(anyString(), anyString());
    }

    @Test
    @DisplayName("updateApplicationSettings：原值为 null 视为空串比较")
    void updateApplicationSettings_nullOldValue() {
        when(settingsCache.getAllApplicationSettingDefinitions())
                .thenReturn(mapOf(definition("A", SettingAccessLevel.Public)));
        when(settingManager.getSettingValueForApplication("A")).thenReturn(null);

        service.updateApplicationSettings(inputOf("A", "x"));

        verify(settingManager).changeSettingForApplication("A", "x");
    }

    @Test
    @DisplayName("updateApplicationSettings：新值为 null 时与空串等价")
    void updateApplicationSettings_nullNewValue() {
        when(settingsCache.getAllApplicationSettingDefinitions())
                .thenReturn(mapOf(definition("A", SettingAccessLevel.Public)));
        when(settingManager.getSettingValueForApplication("A")).thenReturn("");

        service.updateApplicationSettings(inputOf("A", null));

        verify(settingManager, never()).changeSettingForApplication(anyString(), anyString());
    }

    @Test
    @DisplayName("updateTenantSettings：值变化时写入租户级设置")
    void updateTenantSettings() {
        when(settingsCache.getAllTenantSettingDefinitions())
                .thenReturn(mapOf(definition("T", SettingAccessLevel.Login)));
        when(settingManager.getSettingValueForTenant("T")).thenReturn("old");

        service.updateTenantSettings(inputOf("T", "new"));

        verify(settingManager).changeSettingForTenant("T", "new");
    }

    @Test
    @DisplayName("updateStationSettings：未选厂站抛业务异常")
    void updateStationSettings_noStation() {
        assertThatThrownBy(() -> service.updateStationSettings(inputOf("S", "x")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前厂站为空");
    }

    @Test
    @DisplayName("updateStationSettings：已选厂站时写入厂站级设置")
    void updateStationSettings_withStation() {
        DataFilterContextHolder.setDataFilterId("7");
        when(settingsCache.getAllStationSettingDefinitions())
                .thenReturn(mapOf(definition("S", SettingAccessLevel.Login)));
        when(settingManager.getSettingValueForStation("S")).thenReturn("old");

        service.updateStationSettings(inputOf("S", "new"));

        verify(settingManager).changeSettingForStation("S", "new");
    }

    @Test
    @DisplayName("updateUserSettings：值变化时写入用户级设置")
    void updateUserSettings() {
        when(settingsCache.getAllUserSettingDefinitions())
                .thenReturn(mapOf(definition("U", SettingAccessLevel.Login)));
        when(settingManager.getSettingValueForUser("U")).thenReturn("old");

        service.updateUserSettings(inputOf("U", "new"));

        verify(settingManager).changeSettingForUser("U", "new");
    }

    // ------------------------------------------------------------------
    // 发布 / 取值
    // ------------------------------------------------------------------

    @Test
    @DisplayName("publishSettings：写入设置缓存")
    void publishSettings() {
        List<SettingDefinition> definitions = List.of(definition("A", SettingAccessLevel.Public));

        service.publishSettings("app", definitions);

        verify(settingsCache).addSettingDefinitions("app", definitions);
    }

    @Test
    @DisplayName("getValue：委托设置管理器取全局值")
    void getValue() {
        when(settingManager.getSettingValue("A")).thenReturn("v");

        assertThat(service.getValue("A")).isEqualTo("v");
    }

    @Test
    @DisplayName("getValue(name, tenantId)：命中设置返回其值")
    void getValueByTenant_found() {
        Setting setting = new Setting();
        setting.setName("A");
        setting.setValue("tv");
        when(repository.findSettingByNameAndTenantId("A", 3L)).thenReturn(setting);

        assertThat(service.getValue("A", 3L)).isEqualTo("tv");
    }

    @Test
    @DisplayName("getValue(name, tenantId)：未命中返回 null")
    void getValueByTenant_absent() {
        when(repository.findSettingByNameAndTenantId("A", 3L)).thenReturn(null);

        assertThat(service.getValue("A", 3L)).isNull();
    }

    @Test
    @DisplayName("publishSettings：空定义列表也可发布")
    void publishSettings_empty() {
        service.publishSettings("app", List.of());

        ArgumentCaptor<List<SettingDefinition>> captor = ArgumentCaptor.forClass(List.class);
        verify(settingsCache).addSettingDefinitions(org.mockito.ArgumentMatchers.eq("app"), captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("InputType 为空时初始化设置仍可读取")
    void getAllSettingsForInit_nullInputType() {
        SettingDefinition d = definition("plain", SettingAccessLevel.Public);
        d.setInputType((InputType) null);
        when(settingsCache.getAllSettingDefinitions()).thenReturn(mapOf(d));
        when(settingManager.getSettingValueForUser("plain")).thenReturn("v");

        assertThat(service.getAllSettingsForInit()).containsKey("plain");
    }
}
