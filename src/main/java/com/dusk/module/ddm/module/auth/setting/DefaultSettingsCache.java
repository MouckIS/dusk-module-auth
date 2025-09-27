package com.dusk.module.ddm.module.auth.setting;

import com.dusk.common.core.setting.SettingDefinition;
import com.dusk.common.core.setting.SettingScopes;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020/11/27 17:18
 */
@Component
public class DefaultSettingsCache implements ISettingsCache {
    private final static Map<String, Map<String, SettingDefinition>> settingsMap = new HashMap<>();

    @Override
    public synchronized void addSettingDefinitions(String applicationName, List<SettingDefinition> settingDefinitions) {
        Map<String, SettingDefinition> tempSetting = new HashMap<>();
        settingDefinitions.forEach(e -> tempSetting.put(e.getName(), e));
        settingsMap.put(applicationName, tempSetting);
    }

    @Override
    public Map<String, SettingDefinition> getAllSettingDefinitions() {
        Map<String, SettingDefinition> result = new HashMap<>();
        settingsMap.values().forEach(result::putAll);
        return result;
    }

    @Override
    public Map<String, SettingDefinition> getAllApplicationSettingDefinitions() {
        return getAllSettingDefinitions(SettingScopes.Application);
    }

    @Override
    public Map<String, SettingDefinition> getAllTenantSettingDefinitions() {
        return getAllSettingDefinitions(SettingScopes.Tenant);
    }

    @Override
    public Map<String, SettingDefinition> getAllStationSettingDefinitions() {
        return getAllSettingDefinitions(SettingScopes.Station);
    }

    @Override
    public Map<String, SettingDefinition> getAllUserSettingDefinitions() {
        return getAllSettingDefinitions(SettingScopes.User);
    }

    @Override
    public SettingDefinition getSettingDefinition(String name) {
        return getAllSettingDefinitions().get(name);
    }

    private Map<String, SettingDefinition> getAllSettingDefinitions(SettingScopes scope) {
        Map<String, SettingDefinition> result = new HashMap<>();
        getAllSettingDefinitions().forEach((name, definition) -> {
            if (definition.hasScopes(scope)) {
                result.put(name, definition);
            }
        });

        return result;
    }

}
