package com.dusk.module.ddm.module.auth.setting;

import com.dusk.common.core.config.AppConfig;
import com.dusk.common.core.setting.ISettingDefinitionManager;
import com.dusk.common.core.setting.ISettingsPublish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020/11/27 15:47
 */
@Component
@Primary
public class MainSettingsPublish implements ISettingsPublish {
    @Autowired
    private ISettingDefinitionManager settingDefinitionManager;
    @Autowired
    private ISettingsCache settingsCache;
    @Autowired
    private AppConfig appConfig;

    @Override
    public void publishSettings() {
        settingsCache.addSettingDefinitions(appConfig.getApplicationName(), settingDefinitionManager.getAllSettingDefinitions());
    }
}
