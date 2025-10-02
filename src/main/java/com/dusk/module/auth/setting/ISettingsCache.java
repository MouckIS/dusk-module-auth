package com.dusk.module.auth.setting;

import com.dusk.module.ddm.dto.SettingDefinition;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020/11/27 16:02
 */
public interface ISettingsCache {

    void addSettingDefinitions(String applicationName, List<SettingDefinition> settingDefinitions);

    Map<String, SettingDefinition> getAllSettingDefinitions();

    Map<String, SettingDefinition> getAllApplicationSettingDefinitions();

    Map<String, SettingDefinition> getAllTenantSettingDefinitions();

    Map<String, SettingDefinition> getAllStationSettingDefinitions();

    Map<String, SettingDefinition> getAllUserSettingDefinitions();

    SettingDefinition getSettingDefinition(String name);
}
