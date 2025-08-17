package com.dusk.module.auth.setting.provider;

import com.dusk.common.framework.setting.SettingDefinition;
import com.dusk.common.framework.setting.SettingProvider;
import com.dusk.common.framework.setting.SettingScopes;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2023/4/20 11:43
 */
@Component
public class Platform3DSettingProvider extends SettingProvider {
    public static final String PLATFORM_3D_SETTINGS = "Platform.3D.Settings";
    public static final String PLATFORM_3D_SETTINGS_HOST = "Platform.3D.Settings.Host";

    @Override
    public List<SettingDefinition> getSettingDefinitions() {
        ArrayList<SettingDefinition> list = new ArrayList<>();
        list.add(new SettingDefinition.Builder(PLATFORM_3D_SETTINGS, "").displayName("3D系统配置").scopes(SettingScopes.Tenant).inputType(null).build());
        list.add(new SettingDefinition.Builder(PLATFORM_3D_SETTINGS_HOST, "").displayName("服务地址").scopes(SettingScopes.Tenant).parent(PLATFORM_3D_SETTINGS).build());
        return list;
    }
}
