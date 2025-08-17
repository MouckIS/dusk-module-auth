package com.dusk.module.auth.setting;

import com.github.dozermapper.core.Mapper;
import io.seata.common.util.StringUtils;
import com.dusk.common.framework.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.framework.datafilter.DataFilterContextHolder;
import com.dusk.common.framework.jpa.Specifications;
import com.dusk.common.framework.setting.SettingInfo;
import com.dusk.common.framework.setting.SettingScopes;
import com.dusk.common.framework.tenant.TenantContextHolder;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.common.framework.utils.SecurityUtils;
import com.dusk.module.auth.entity.Setting;
import com.dusk.module.auth.repository.ISettingRepository;
import com.dusk.module.auth.service.ITenantService;
import com.dusk.module.auth.setting.config.MultiTenancyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author kefuming
 * @date 2020-06-17 10:33
 */
@Component
public class SettingManagerDefault implements ISettingManager {
    @Autowired
    protected ISettingsCache settingsCache;
    @Autowired
    protected ITenantService tenantService;
    @Autowired
    protected SecurityUtils securityUtils;
    @Autowired
    private Mapper mapper;
    @Autowired
    private ISettingRepository settingRepository;

    @Autowired
    protected MultiTenancyConfig multiTenancyConfig;

    @Override
    public String getSettingValue(String name) {
        return getSettingValueInternal(name, getTenantId(), getStationId(), getCurrentUserId());
    }

    @Override
    public String getSettingValueForApplication(String name) {
        return getSettingValueInternal(name);
    }

    @Override
    public String getSettingValueForTenant(String name) {
        return getSettingValueInternal(name, getTenantId());
    }

    @Override
    public String getSettingValueForStation(String name) {
        return getSettingValueInternal(name, getTenantId(), getStationId());
    }

    @Override
    public String getSettingValueForUser(String name) {
        return getSettingValueInternal(name, getTenantId(), getStationId(), getCurrentUserId());
    }

    @Override
    public void changeSettingForApplication(String name, String value) {
        if (multiTenancyConfig.isEnable()) {
            insertOrUpdateOrDeleteSettingValue(name, value, null, null, null);
        } else {
            // If MultiTenancy is disabled, then we should change default tenant's setting
            changeSettingForTenant(name, value);
        }
    }

    @Override
    public void changeSettingForTenant(String name, String value) {
        insertOrUpdateOrDeleteSettingValue(name, value, getTenantId(), null, null);
    }

    @Override
    public void changeSettingForStation(String name, String value) {
        insertOrUpdateOrDeleteSettingValue(name, value, getTenantId(), getStationId(), null);
    }

    @Override
    public void changeSettingForUser(String name, String value) {
        insertOrUpdateOrDeleteSettingValue(name, value, getTenantId(), getStationId(), getCurrentUserId());
    }

    private String getSettingValueInternal(String name) {
        return getSettingValueInternal(name, null, null, null);
    }

    private String getSettingValueInternal(String name, Long tenantId) {
        return getSettingValueInternal(name, tenantId, null, null);
    }

    private String getSettingValueInternal(String name, Long tenantId, Long stationId) {
        return getSettingValueInternal(name, tenantId, stationId, null);
    }

    private String getSettingValueInternal(String name, Long tenantId, Long stationId, Long userId) {
        return getSettingValueInternal(name, tenantId, stationId, userId, true);
    }

    private String getSettingValueInternal(String name, Long tenantId, Long stationId, Long userId, boolean fallbackToDefault) {
        var settingDefinition = settingsCache.getSettingDefinition(name);

        //Get for user if defined
        if (settingDefinition.hasScopes(SettingScopes.User) && userId != null) {
            var settingValue = getSettingValueForUserOrNull(name);
            if (settingValue != null) {
                return settingValue.getValue();
            }

            if (!fallbackToDefault) {
                return null;
            }

            if (!settingDefinition.isInherited()) {
                return settingDefinition.getDefaultValue();
            }
        }

        //Get for station if defined
        if (settingDefinition.hasScopes(SettingScopes.Station) && stationId != null) {
            var settingValue = getSettingValueForStationOrNull(name);
            if (settingValue != null) {
                return settingValue.getValue();
            }

            if (!fallbackToDefault) {
                return null;
            }

            if (!settingDefinition.isInherited()) {
                return settingDefinition.getDefaultValue();
            }
        }

        //Get for tenant if defined
        if (settingDefinition.hasScopes(SettingScopes.Tenant) && tenantId != null) {
            var settingValue = getSettingValueForTenantOrNull(name);
            if (settingValue != null) {
                return settingValue.getValue();
            }

            if (!fallbackToDefault) {
                return null;
            }

            if (!settingDefinition.isInherited()) {
                return settingDefinition.getDefaultValue();
            }
        }

        //Get for application if defined
        if (settingDefinition.hasScopes(SettingScopes.Application)) {
            var settingValue = getSettingValueForApplicationOrNull(name);
            if (settingValue != null) {
                return settingValue.getValue();
            }

            if (!fallbackToDefault) {
                return null;
            }
        }

        //Not defined, get default value
        return settingDefinition.getDefaultValue();
    }


    private SettingInfo getSettingValueForUserOrNull(String name) {
        return getUserSettings().get(name);
    }

    private SettingInfo getSettingValueForTenantOrNull(String name) {
        return getTenantSettings().get(name);
    }

    private SettingInfo getSettingValueForStationOrNull(String name) {
        return getStationSettings().get(name);
    }

    private SettingInfo getSettingValueForApplicationOrNull(String name) {
        if (multiTenancyConfig.isEnable()) {
            return (getApplicationSettings()).get(name);
        }

        return getTenantSettings().get(name);
    }

    protected Map<String, SettingInfo> getUserSettings() {
        Map<String, SettingInfo> finalResult = new HashMap<>();
        getAllSettings(getTenantId(), getStationId(), getCurrentUserId()).forEach(settingInfo -> finalResult.put(settingInfo.getName(), settingInfo));
        return finalResult;
    }

    protected Map<String, SettingInfo> getStationSettings() {
        Map<String, SettingInfo> finalResult = new HashMap<>();
        getAllSettings(getTenantId(), getStationId(), null).forEach(settingInfo -> finalResult.put(settingInfo.getName(), settingInfo));
        return finalResult;
    }

    protected Map<String, SettingInfo> getTenantSettings() {
        Long tenantId = getTenantId();
        Map<String, SettingInfo> finalResult = new HashMap<>();
        if (tenantId == null) {
            return finalResult;
        }

        if (!multiTenancyConfig.isEnable() && tenantService.findById(tenantId).isEmpty()) {
            return finalResult;
        }
        getAllSettings(tenantId, null, null).forEach(settingInfo -> finalResult.put(settingInfo.getName(), settingInfo));
        return finalResult;
    }

    protected Map<String, SettingInfo> getApplicationSettings() {
        Map<String, SettingInfo> finalResult = new HashMap<>();
        getAllSettings(null, null, null).forEach(settingInfo -> finalResult.put(settingInfo.getName(), settingInfo));
        return finalResult;
    }

    protected SettingInfo insertOrUpdateOrDeleteSettingValue(String name, String value, Long tenantId, Long stationId, Long userId) {
        if (value == null) {
            value = "";
        }
        var settingDefinition = settingsCache.getSettingDefinition(name);
        var settingValue = getSettingOrNull(tenantId, stationId, userId, name);

        //Determine defaultValue
        var defaultValue = settingDefinition.getDefaultValue();

        if (settingDefinition.isInherited()) {
            //For Tenant, Station and User, Application's value overrides Setting Definition's default value when multi tenancy is enabled.
            if (multiTenancyConfig.isEnable() && tenantId != null) {
                var applicationValue = getSettingValueForApplicationOrNull(name);
                if (applicationValue != null) {
                    defaultValue = applicationValue.getValue();
                }
            }

            //For Station, Tenants's value overrides Application's default value.
            if (stationId != null && tenantId != null) {
                var tenantValue = getSettingValueForTenantOrNull(name);
                if (tenantValue != null) {
                    defaultValue = tenantValue.getValue();
                }
            }

            //For User, Station's value overrides Tenant's default value.
            if (userId != null && stationId != null) {
                var stationValue = getSettingValueForStationOrNull(name);
                if (stationValue != null) {
                    defaultValue = stationValue.getValue();
                }
            }
        }

        //No need to store on database if the value is the default value
        if (defaultValue.equals(value)) {
            if (settingValue != null) {
                delete(settingValue);
            }
            return null;
        }

        //If it's not default value and not stored on database, then insert it
        if (settingValue == null) {
            settingValue = new SettingInfo(tenantId, stationId, userId, name, value);
            create(settingValue);
            return settingValue;
        }

        //It's same value in database, no need to update
        if (settingValue.getValue().equals(value)) {
            return settingValue;
        }

        //Update the setting on database.
        settingValue.setValue(value);
        update(settingValue);

        return settingValue;
    }

    /**
     * 获取当前用户id
     *
     * @return
     */
    protected Long getCurrentUserId() {
        return LoginUserIdContextHolder.getUserId();
    }

    /**
     * 当前租户id
     * @return
     */
    protected Long getTenantId() {
        return TenantContextHolder.getTenantId();
    }

    /**
     * 当前厂站id
     * @return
     */
    protected Long getStationId() {
        return DataFilterContextHolder.getDefaultOrgId();
    }

    private List<SettingInfo> getAllSettings(Long tenantId, Long stationId, Long userId) {
        Specification<Setting> query = createQuery(tenantId, stationId, userId, null);
        return DozerUtils.mapList(mapper, settingRepository.findAll(query), SettingInfo.class);
    }

    private Specification<Setting> createQuery(Long tenantId, Long stationId, Long userId, String name) {
        return Specifications.where(e -> {
            e.eq(tenantId != null, Setting.Fields.tenantId, tenantId).isNull(tenantId == null, Setting.Fields.tenantId).eq(stationId != null, Setting.Fields.stationId, stationId)
                    .isNull(stationId == null, Setting.Fields.stationId).eq(userId != null, Setting.Fields.userId, userId).isNull(userId == null, Setting.Fields.userId)
                    .eq(StringUtils.isNotBlank(name), Setting.Fields.name, name);
        });
    }

    private SettingInfo getSettingOrNull(Long tenantId, Long stationId, Long userId, String name) {
        Optional<Setting> optional = settingRepository.findOne(createQuery(tenantId, stationId, userId, name));
        return optional.isEmpty() ? null : mapper.map(optional.get(), SettingInfo.class);
    }

    public void delete(SettingInfo settingInfo) {
        List<Setting> settings = settingRepository.findAll(createQuery(settingInfo));
        settingRepository.deleteInBatch(settings);
    }

    public void create(SettingInfo settingInfo) {
        settingRepository.save(mapper.map(settingInfo, Setting.class));
    }

    public void update(SettingInfo settingInfo) {
        Optional<Setting> optional = settingRepository.findOne(createQuery(settingInfo));
        if (optional.isPresent()) {
            Setting setting = optional.get();
            setting.setValue(settingInfo.getValue());
            settingRepository.save(setting);
        }
    }

    private Specification<Setting> createQuery(SettingInfo settingInfo) {
        return createQuery(settingInfo.getTenantId(), settingInfo.getStationId(), settingInfo.getUserId(), settingInfo.getName());
    }
}
