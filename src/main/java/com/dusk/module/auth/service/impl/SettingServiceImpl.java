package com.dusk.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import io.seata.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.framework.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.framework.datafilter.DataFilterContextHolder;
import com.dusk.common.framework.dto.NameValueDto;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.feature.ui.FileInput;
import com.dusk.common.framework.feature.ui.InputType;
import com.dusk.common.framework.setting.SettingAccessLevel;
import com.dusk.common.framework.setting.SettingDefinition;
import com.dusk.common.framework.utils.SecurityUtils;
import com.dusk.common.module.auth.service.ISettingRpcService;
import com.dusk.common.module.minio.dto.GetDownloadUrlOutput;
import com.dusk.common.module.minio.service.IMinioRpcService;
import com.dusk.module.auth.dto.setting.SettingDto;
import com.dusk.module.auth.dto.setting.UpdateSettingInput;
import com.dusk.module.auth.entity.Setting;
import com.dusk.module.auth.repository.ISettingRepository;
import com.dusk.module.auth.service.ISettingService;
import com.dusk.module.auth.setting.ISettingManager;
import com.dusk.module.auth.setting.ISettingsCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-05-21 15:12
 */
@Service
@Transactional
@Slf4j
public class SettingServiceImpl implements ISettingRpcService, ISettingService {
    @Autowired
    private ISettingRepository repository;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    private ISettingsCache settingsCache;
    @Autowired
    private ISettingManager settingManager;
    @Autowired
    private SecurityUtils securityUtils;
    @Reference
    private IMinioRpcService minioRpcService;

    @Override
    public List<SettingDto> getApplicationSettings() {
        List<SettingDto> result = new ArrayList<>();
        Map<String, SettingDefinition> settingDefinitionMap = settingsCache.getAllApplicationSettingDefinitions();
        settingDefinitionMap.forEach((name, definition) -> {
            SettingDto setting = dozerMapper.map(definition, SettingDto.class);
            setting.setValue(settingManager.getSettingValueForApplication(name));
            result.add(setting);
        });
        handlerFileInput(result);

        return result;
    }

    @Override
    public List<SettingDto> getTenantSettings() {
        List<SettingDto> result = new ArrayList<>();
        Map<String, SettingDefinition> settingDefinitionMap = settingsCache.getAllTenantSettingDefinitions();
        settingDefinitionMap.forEach((name, definition) -> {
            SettingDto setting = dozerMapper.map(definition, SettingDto.class);
            setting.setValue(settingManager.getSettingValueForTenant(name));
            result.add(setting);
        });
        handlerFileInput(result);

        return result;
    }

    @Override
    public List<SettingDto> getStationSettings() {
        List<SettingDto> result = new ArrayList<>();
        Map<String, SettingDefinition> settingDefinitionMap = settingsCache.getAllStationSettingDefinitions();
        settingDefinitionMap.forEach((name, definition) -> {
            SettingDto setting = dozerMapper.map(definition, SettingDto.class);
            setting.setValue(settingManager.getSettingValueForStation(name));
            result.add(setting);
        });
        handlerFileInput(result);

        return result;
    }

    @Override
    public List<SettingDto> getUserSettings() {
        List<SettingDto> result = new ArrayList<>();
        Map<String, SettingDefinition> settingDefinitionMap = settingsCache.getAllUserSettingDefinitions();
        settingDefinitionMap.forEach((name, definition) -> {
            SettingDto setting = dozerMapper.map(definition, SettingDto.class);
            setting.setValue(settingManager.getSettingValueForUser(name));
            result.add(setting);
        });
        handlerFileInput(result);

        return result;

    }

    @Override
    public Map<String, Map<String, String>> getAllSettingsForInit() {
        if(LoginUserIdContextHolder.getUserId() == null){
            return getPublicSettings();
        }
        return getLoginAccessableSettings();
    }

    private Map<String, Map<String, String>> getPublicSettings() {
        return getSettings(getPublicSettingDefinitions());
    }

    private Map<String, Map<String, String>> getLoginAccessableSettings() {
        return getSettings(getLoginAccessableSettingDefinitions());
    }

    @Override
    public void updateApplicationSettings(UpdateSettingInput input) {
        List<SettingDto> settings = getApplicationSettings();
        for (NameValueDto<String> nameValue : input.getNameValues()) {
            for (SettingDto setting : settings) {
                if (nameValue.getName().equals(setting.getName()) && valueChanged(setting, nameValue.getValue())) {
                    settingManager.changeSettingForApplication(nameValue.getName(), nameValue.getValue());
                }
            }
        }
    }

    @Override
    public void updateTenantSettings(UpdateSettingInput input) {
        List<SettingDto> settings = getTenantSettings();
        for (NameValueDto<String> nameValue : input.getNameValues()) {
            for (SettingDto setting : settings) {
                if (nameValue.getName().equals(setting.getName()) && valueChanged(setting, nameValue.getValue())) {
                    settingManager.changeSettingForTenant(nameValue.getName(), nameValue.getValue());
                }
            }
        }
    }

    @Override
    public void updateStationSettings(UpdateSettingInput input) {
        if (DataFilterContextHolder.getDefaultOrgId() == null) {
            throw new BusinessException("当前厂站为空！");
        }
        List<SettingDto> settings = getStationSettings();
        for (NameValueDto<String> nameValue : input.getNameValues()) {
            for (SettingDto setting : settings) {
                if (nameValue.getName().equals(setting.getName()) && valueChanged(setting, nameValue.getValue())) {
                    settingManager.changeSettingForStation(nameValue.getName(), nameValue.getValue());
                }
            }
        }
    }

    @Override
    public void updateUserSettings(UpdateSettingInput input) {
        List<SettingDto> settings = getUserSettings();
        for (NameValueDto<String> nameValue : input.getNameValues()) {
            for (SettingDto setting : settings) {
                if (nameValue.getName().equals(setting.getName()) && valueChanged(setting, nameValue.getValue())) {
                    settingManager.changeSettingForUser(nameValue.getName(), nameValue.getValue());
                }
            }
        }
    }

    @Override
    public void publishSettings(String applicationName, List<SettingDefinition> settingDefinitions) {
        settingsCache.addSettingDefinitions(applicationName, settingDefinitions);
    }

    @Override
    public String getValue(String name) {
        return settingManager.getSettingValue(name);
    }

    @Override
    public String getValue(String name, Long tenantId) {
        Setting setting = repository.findSettingByNameAndTenantId(name, tenantId);
        return setting!=null ? setting.getValue() : null;
    }

    private void handlerFileInput(List<SettingDto> settings) {
        List<SettingDto> fileSettings = settings.stream()
                .filter(e -> e.getInputType() != null && FileInput.NAME.equals(e.getInputType().getName()) && StringUtils.isNotBlank(e.getValue())).collect(Collectors.toList());
        if (fileSettings.size() > 0) {
            try {
                List<GetDownloadUrlOutput> downloadUrls = minioRpcService
                        .getDownloadUrls(fileSettings.stream().map(e -> Long.parseLong(e.getValue())).collect(Collectors.toList()));
                fileSettings.forEach(e -> {
                    downloadUrls.stream().filter(d -> d.getId().toString().equals(e.getValue())).findFirst().ifPresent(d -> {
                        e.setFileName(d.getFileName());
                        e.setDownloadUrl(d.getDownloadUrl());
                    });
                });
            } catch (Exception e) {
                fileSettings.forEach(m -> {
                    m.setFileName("minio服务异常");
                });
            }
        }
    }

    private boolean valueChanged(SettingDto setting, String newValue) {
        String oValue = setting.getValue() == null ? "" : setting.getValue();
        String nValue = newValue == null ? "" : newValue;
        return !oValue.equals(nValue);
    }


    private Map<String, SettingDefinition> getPublicSettingDefinitions(){
        return filterDefinitions(SettingAccessLevel.Public);
    }

    private Map<String, SettingDefinition> getLoginAccessableSettingDefinitions(){
        return filterDefinitions(SettingAccessLevel.Login);
    }

    private Map<String, SettingDefinition> filterDefinitions(SettingAccessLevel accessLevel){
        Map<String, SettingDefinition> result = new HashMap<>();
        Map<String, SettingDefinition> all = settingsCache.getAllSettingDefinitions();
        all.forEach((k, definition) -> {
            if(definition.getAccessLevel().getValue() <= accessLevel.getValue()){
                result.put(k, definition);
            }
        });
        return result;
    }

    private Map<String, Map<String, String>> getSettings(Map<String, SettingDefinition> definitionMap) {
        Map<String, Map<String, String>> resultMap = new HashMap<>();
        List<Map<String, String>> fileMapList = new ArrayList<>();
        definitionMap.forEach((k, v) -> {
            String name = v.getName();
            String value = settingManager.getSettingValueForUser(name);
            Map<String, String> temMap = new HashMap<>();
            temMap.put("value", value);
            InputType inputType = v.getInputType();
            if (inputType != null && FileInput.NAME.equals(inputType.getName())) {
                if (StringUtils.isNotBlank(value)) {
                    fileMapList.add(temMap);
                }
            }
            resultMap.put(name, temMap);
        });
        if (!fileMapList.isEmpty()) {
            List<Long> fileIds = fileMapList.stream().map(temMap -> Long.valueOf(temMap.get("value"))).collect(Collectors.toList());
            try {
                List<GetDownloadUrlOutput> downloadUrls = minioRpcService.getDownloadUrls(fileIds);
                fileMapList.forEach(temMap -> {
                    Long id = Long.valueOf(temMap.get("value"));
                    downloadUrls.stream().filter(url -> id.equals(url.getId())).findAny().ifPresent(url -> {
                        temMap.put("fileName", url.getFileName());
                        temMap.put("downloadUrl", url.getDownloadUrl());
                    });
                });
            } catch (Exception ex) {
                log.error("获取关联minio文件出错:{}", ex.toString());
            }

        }
        return resultMap;
    }
}
