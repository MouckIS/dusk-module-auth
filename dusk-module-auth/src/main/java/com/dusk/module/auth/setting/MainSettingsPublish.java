package com.dusk.module.auth.setting;

import com.dusk.module.ddm.service.ISettingDefinitionManager;
import com.dusk.module.ddm.service.ISettingsPublish;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Resource
    private ISettingsCache settingsCache;
    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public void publishSettings() {
        settingsCache.addSettingDefinitions(applicationName, settingDefinitionManager.getAllSettingDefinitions());
    }
}
