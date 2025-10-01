package com.dusk.module.auth.setting.provider;

import com.dusk.module.ddm.dto.SettingDefinition;
import com.dusk.module.ddm.enums.SettingScopes;
import com.dusk.module.ddm.provider.SettingProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: kefuming
 * @date: 2022-06-07 9:32
 */
@Component
public class ApplicationSettingProvider extends SettingProvider {
    public static final String APPLICATION_SETTINGS = "Application.Settings";
    public static final String APPLICATION_SETTINGS_NAME = "Application.Settings.Name";
    public static final String APPLICATION_SETTINGS_DESCRIPTION = "Application.Settings.Description";

    @Override
    public List<SettingDefinition> getSettingDefinitions() {
        return new ArrayList<>(){{
            add(new SettingDefinition.Builder(APPLICATION_SETTINGS, "").displayName("系统设置").scopes(SettingScopes.Application).inputType(null).build());
            add(new SettingDefinition.Builder(APPLICATION_SETTINGS_NAME,"权限管理中心").displayName("应用名称").scopes(SettingScopes.Application).parent(APPLICATION_SETTINGS).build());
            add(new SettingDefinition.Builder(APPLICATION_SETTINGS_DESCRIPTION,"").displayName("应用描述").scopes(SettingScopes.Application).parent(APPLICATION_SETTINGS).build());
        }};
    }
}
