package com.dusk.module.auth.setting;

import com.dusk.common.mqs.config.AppConfig;
import com.dusk.module.ddm.service.ISettingDefinitionManager;
import com.dusk.module.ddm.service.ISettingsPublish;
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
    @Autowired(required = false)
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
