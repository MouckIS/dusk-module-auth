package com.dusk.module.auth.setting.provider;

import com.dusk.module.ddm.dto.SettingDefinition;
import com.dusk.module.ddm.dto.ui.CheckBox;
import com.dusk.module.ddm.enums.SettingScopes;
import com.dusk.module.ddm.provider.SettingProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Host站点的配置
 *
 * @author kefuming
 * @date 2022/1/12 10:38
 */
@Component
public class HostSettingProvider extends SettingProvider {

    public static final String HOST_SETTINGS = "Host.Settings";
    public static final String HOST_DOMAIN = "Host.Settings.Domain";
    public static final String HOST_SCHEMA = "Host.Settings.SCHEMA";

    @Override
    public List<SettingDefinition> getSettingDefinitions() {
        return new ArrayList<>(){{
            add(new SettingDefinition.Builder(HOST_SETTINGS, "").displayName("站点配置").scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(null).build());
            add(new SettingDefinition.Builder(HOST_DOMAIN,"").displayName("域名/IP").scopes(SettingScopes.Application, SettingScopes.Tenant).parent(HOST_SETTINGS).build());
            add(new SettingDefinition.Builder(HOST_SCHEMA,"false").displayName("使用HTTPS").scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(new CheckBox()).parent(HOST_SETTINGS).build());
        }};
    }
}
