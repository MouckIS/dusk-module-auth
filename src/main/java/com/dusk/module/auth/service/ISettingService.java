package com.dusk.module.auth.service;

import com.dusk.module.auth.dto.setting.SettingDto;
import com.dusk.module.auth.dto.setting.UpdateSettingInput;
import com.dusk.module.ddm.service.ISettingRpcService;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-05-21 9:02
 */
public interface ISettingService extends ISettingRpcService {
    /**
     * 获取当前用户的配置
     * @return
     */
    List<SettingDto> getApplicationSettings();

    /**
     * 获取当前租户的配置
     * @return
     */
    List<SettingDto> getTenantSettings();

    /**
     * 获取当前厂站的配置
     * @return
     */
    List<SettingDto> getStationSettings();

    /**
     * 获取当前用户的配置
     * @return
     */
    List<SettingDto> getUserSettings();

    /**
     * 仅限于前端调用，不包含仅后端可访问的配置
     * @return
     */
    Map<String, Map<String, String>> getAllSettingsForInit();

    /**
     * 更新应用的配置信息
     * @param input
     */
    void updateApplicationSettings(UpdateSettingInput input);

    /**
     * 更新租户的配置信息
     * @param input
     */
    void updateTenantSettings(UpdateSettingInput input);

    /**
     * 更新厂站的配置信息
     * @param input
     */
    void updateStationSettings(UpdateSettingInput input);

    /**
     * 更新用户的配置信息
     * @param input
     */
    void updateUserSettings(UpdateSettingInput input);
}
