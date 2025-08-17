package com.dusk.module.auth.setting;

import com.dusk.common.framework.lock.annotation.Lock4j;
import com.dusk.common.framework.redis.RedisCacheCondition;
import com.dusk.common.framework.redis.RedisUtil;
import com.dusk.common.framework.setting.SettingDefinition;
import com.dusk.common.framework.setting.SettingScopes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020/11/27 16:46
 */
@Conditional(RedisCacheCondition.class)
@Component
@Primary
public class RedisSettingsCache implements ISettingsCache {
    private static final String SETTING_DEFINITION_KEY = "CRUX:SYS:SETTING:DEFINITION:MAPS";
    @Autowired(required = false)
    RedisUtil<Object> redisUtil;


    @Override
    @Lock4j
    public void addSettingDefinitions(String applicationName, List<SettingDefinition> settingDefinitions) {
        Map<String, Map<String, SettingDefinition>> definitionsMap = getCacheSettingDefinitions();
        Map<String, SettingDefinition> tempSetting = new HashMap<>();
        settingDefinitions.forEach(e -> tempSetting.put(e.getName(), e));
        definitionsMap.put(applicationName, tempSetting);
        redisUtil.setCache(SETTING_DEFINITION_KEY, definitionsMap);
    }


    @Override
    public Map<String, SettingDefinition> getAllApplicationSettingDefinitions() {
        return getAllSettingDefinitions(SettingScopes.Application);
    }

    @Override
    public Map<String, SettingDefinition> getAllTenantSettingDefinitions() {
        return getAllSettingDefinitions(SettingScopes.Tenant);
    }

    @Override
    public Map<String, SettingDefinition> getAllStationSettingDefinitions() {
        return getAllSettingDefinitions(SettingScopes.Station);
    }

    @Override
    public Map<String, SettingDefinition> getAllUserSettingDefinitions() {
        return getAllSettingDefinitions(SettingScopes.User);
    }

    @Override
    public SettingDefinition getSettingDefinition(String name) {
        return getAllSettingDefinitions().get(name);
    }

    private Map<String, SettingDefinition> getAllSettingDefinitions(SettingScopes scope) {
        Map<String, SettingDefinition> result = new HashMap<>();
        Map<String, SettingDefinition> definitionsMap = getAllSettingDefinitions();
        definitionsMap.forEach((name, definition) -> {
            if (definition.hasScopes(scope)) {
                result.put(name, definition);
            }
        });

        return result;
    }

    //获取打平的业务设置
    @Override
    public Map<String, SettingDefinition> getAllSettingDefinitions() {
        Map<String, SettingDefinition> result = new HashMap<>();
        Map<String, Map<String, SettingDefinition>> allSettingDefinitions = getCacheSettingDefinitions();
        allSettingDefinitions.values().forEach(result::putAll);
        return result;
    }


    private Map<String, Map<String, SettingDefinition>> getCacheSettingDefinitions() {
        try {
            Object cache = redisUtil.getCache(SETTING_DEFINITION_KEY);
            if (cache != null) {
                return (Map<String, Map<String, SettingDefinition>>) cache;
            }
        } catch (Exception ignored) {

        }
        return new HashMap<>();
    }

}
