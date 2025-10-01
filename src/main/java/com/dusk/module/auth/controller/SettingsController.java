package com.dusk.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.module.auth.authorization.HostSettingsAuthProvider;
import com.dusk.module.auth.authorization.TenantSettingsAuthProvider;
import com.dusk.module.auth.dto.setting.SettingDto;
import com.dusk.module.auth.dto.setting.UpdateSettingInput;
import com.dusk.module.auth.service.ISettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020/11/27 10:33
 */
@RestController
@RequestMapping("/Settings")
@Api(description = "配置", tags = "settings")
public class SettingsController extends CruxBaseController {
    @Autowired
    private ISettingService settingService;

    @GetMapping("getApplicationSettings")
    @ApiOperation("获取应用的配置信息")
    @AllowAnonymous
    public List<SettingDto> getApplicationSettings() {
        return settingService.getApplicationSettings();
    }

    @GetMapping("getTenantSettings")
    @ApiOperation("获取租户的配置信息")
    @AllowAnonymous
    public List<SettingDto> getTenantSettings() {
        return settingService.getTenantSettings();
    }

    @GetMapping("getStationSettings")
    @ApiOperation("获取厂站的配置信息")
    @AllowAnonymous
    public List<SettingDto> getStationSettings() {
        return settingService.getStationSettings();
    }

    @GetMapping("getUserSettings")
    @ApiOperation("获取用户的配置信息")
    public List<SettingDto> getUserSettings() {
        return settingService.getUserSettings();
    }

    @GetMapping("getAllSettingsForInit")
    @ApiOperation("获取所有配置用于初始化")
    @AllowAnonymous
    public Map<String, Map<String, String>> getAllSettingsForInit() {
        return settingService.getAllSettingsForInit();
    }

    @PostMapping("updateApplicationSettings")
    @ApiOperation("更新应用的配置信息")
    @Authorize(HostSettingsAuthProvider.PAGES_ADMINISTRATION_HOST_MAINTENANCE)
    public void updateApplicationSettings(@Valid @RequestBody UpdateSettingInput input) {
        settingService.updateApplicationSettings(input);
    }

    @PostMapping("updateTenantSettings")
    @ApiOperation("更新租户的配置信息")
    @Authorize(TenantSettingsAuthProvider.PAGES_ADMINISTRATION_TENANT_SETTINGS)
    public void updateTenantSettings(@Valid @RequestBody UpdateSettingInput input) {
        settingService.updateTenantSettings(input);
    }

    @PostMapping("updateStationSettings")
    @ApiOperation("更新厂站的配置信息")
    @Authorize(TenantSettingsAuthProvider.PAGES_ADMINISTRATION_STATION_SETTINGS)
    public void updateStationSettings(@Valid @RequestBody UpdateSettingInput input) {
        settingService.updateStationSettings(input);
    }

    @PostMapping("updateUserSettings")
    @ApiOperation("更新用户的配置信息")
    public void updateUserSettings(@Valid @RequestBody UpdateSettingInput input) {
        settingService.updateUserSettings(input);
    }

}
