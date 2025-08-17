package com.dusk.module.auth.cache.impl;

import cn.hutool.core.util.StrUtil;
import com.dusk.module.auth.cache.IUserWxRelationCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author kefuming
 * @date 2021-07-23 15:57
 */
@Service
public class UserWxRelationCacheServiceImpl implements IUserWxRelationCacheService {
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    //CRUX:AUTH:WX:RELATION:{用户id}
    final String REDIS_KEY = "CRUX:AUTH:WX:RELATION:{}";
    //缓存时间
    final long EXPIRE_TIME = 30;


    @Override
    public void saveWxRelation(Long userId, String appId, String openId) {
        if (redisTemplate == null) {
            return;
        }
        String redisKey = StrUtil.format(REDIS_KEY, userId);
        redisTemplate.opsForHash().put(redisKey, appId, openId);
        redisTemplate.expire(redisKey, EXPIRE_TIME, TimeUnit.DAYS);
    }

    @Override
    public void saveWxRelationMap(Long userId, Map<String, String> hv) {
        if (redisTemplate == null) {
            return;
        }
        String redisKey = StrUtil.format(REDIS_KEY, userId);
        redisTemplate.opsForHash().putAll(redisKey, hv);
        redisTemplate.expire(redisKey, EXPIRE_TIME, TimeUnit.DAYS);
    }

    @Override
    public String getWxRelation(Long userId, String appId) {
        if (redisTemplate == null) {
            return null;
        }
        String redisKey = StrUtil.format(REDIS_KEY, userId);
        String hv = (String) redisTemplate.opsForHash().get(redisKey, appId);
        redisTemplate.expire(redisKey, EXPIRE_TIME, TimeUnit.DAYS);
        return hv;
    }

    @Override
    public Map<Object, Object> getWxRelationMap(Long userId) {
        if (redisTemplate == null) {
            return null;
        }
        String redisKey = StrUtil.format(REDIS_KEY, userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
        redisTemplate.expire(redisKey, EXPIRE_TIME, TimeUnit.DAYS);
        return entries;
    }
}
