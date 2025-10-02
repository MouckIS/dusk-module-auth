package com.dusk.module.auth.setting;

import com.dusk.common.core.redis.RedisCacheCondition;
import com.dusk.common.core.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author kefuming
 * @date 2020-06-17 10:32
 */
@Conditional(RedisCacheCondition.class)
@Component
@Primary
public class SettingManagerCache extends SettingManagerDefault {
    private static final String APPLICATION_SETTINGS_CACHE_KEY = "CRUX:SETTINGS:APPLICATION";
    private static final String TENANT_SETTINGS_CACHE_KEY_PREFIX = "CRUX:SETTINGS:TENANT:";
    private static final String STATION_SETTINGS_CACHE_KEY_PREFIX = "CRUX:SETTINGS:STATION:";
    private static final String USER_SETTINGS_CACHE_KEY_PREFIX = "CRUX:SETTINGS:USER:";

    @Autowired(required = false)
    private RedisUtil<Object> redisUtil;

    @Override
    public void changeSettingForApplication(String name, String value) {
        if (multiTenancyConfig.isEnable()){
            insertOrUpdateOrDeleteSettingValue(name, value, null, null, null);
        }else{
            // If MultiTenancy is disabled, then we should change default tenant's setting
            changeSettingForTenant(name, value);
        }
        redisUtil.setExpire(APPLICATION_SETTINGS_CACHE_KEY, -1);
    }

    @Override
    public void changeSettingForTenant(String name, String value) {
        insertOrUpdateOrDeleteSettingValue(name, value, getTenantId(), null, null);
        redisUtil.setExpire(TENANT_SETTINGS_CACHE_KEY_PREFIX + getTenantId(), -1);
    }

    @Override
    public void changeSettingForStation(String name, String value) {
        insertOrUpdateOrDeleteSettingValue(name, value, getTenantId(), getStationId(), null);
        redisUtil.setExpire(STATION_SETTINGS_CACHE_KEY_PREFIX + getStationId(), -1);
    }

    @Override
    public void changeSettingForUser(String name, String value) {
        insertOrUpdateOrDeleteSettingValue(name, value, getTenantId(), getStationId(), getCurrentUserId());
        redisUtil.setExpire(USER_SETTINGS_CACHE_KEY_PREFIX + getCurrentUserId(), -1);
    }

    @Override
    protected Map<String, SettingInfo> getUserSettings(){
        Map<String, SettingInfo> result = convertCache(getCache(USER_SETTINGS_CACHE_KEY_PREFIX + getCurrentUserId()));
        if(result == null){
            result = super.getUserSettings();
            redisUtil.setCache(USER_SETTINGS_CACHE_KEY_PREFIX + getCurrentUserId(), result);
        }
        return result;
    }

    @Override
    protected Map<String, SettingInfo> getStationSettings(){
        Map<String, SettingInfo> result = convertCache(getCache(STATION_SETTINGS_CACHE_KEY_PREFIX + getStationId()));
        if(result == null){
            result = super.getStationSettings();
            redisUtil.setCache(STATION_SETTINGS_CACHE_KEY_PREFIX + getStationId(), result);
        }
        return result;
    }

    @Override
    protected Map<String, SettingInfo> getTenantSettings(){
        Map<String, SettingInfo> result = convertCache(getCache(TENANT_SETTINGS_CACHE_KEY_PREFIX + getTenantId()));
        if(result == null){
            result = super.getTenantSettings();
            redisUtil.setCache(TENANT_SETTINGS_CACHE_KEY_PREFIX + getTenantId(), result);
        }
        return result;
    }

    protected Map<String, SettingInfo> getApplicationSettings(){
        Map<String, SettingInfo> result = convertCache(getCache(APPLICATION_SETTINGS_CACHE_KEY));
        if(result == null){
            result = super.getApplicationSettings();
            redisUtil.setCache(APPLICATION_SETTINGS_CACHE_KEY, result);
        }
        return result;
    }

    private Object getCache(String key){
        try{
            return redisUtil.getCache(key);
        }catch (Exception e){
            return null;
        }
    }

    private Map<String, SettingInfo> convertCache(Object cache){
        if(cache != null){
            try {
                return (Map<String, SettingInfo>)cache;
            }catch (Exception ignored){
            }
        }
        return null;
    }
}
