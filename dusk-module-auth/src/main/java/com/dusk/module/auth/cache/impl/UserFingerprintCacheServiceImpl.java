package com.dusk.module.auth.cache.impl;

import cn.hutool.core.util.StrUtil;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.module.auth.cache.IUserFingerprintCacheService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author kefuming
 * @date 2021-05-12 8:16
 */
@Service
public class UserFingerprintCacheServiceImpl implements IUserFingerprintCacheService {
    private final String USER_SEQ_REDIS_KEY = "CRUX:AUTH:FINGERPRINT:USERSEQ:{}";
    @Resource
    private RedisUtil<Integer> redisUtil;

    @Override
    public Integer getUserSeq(Long userId) {
        return redisUtil.getCache(StrUtil.format(USER_SEQ_REDIS_KEY, userId));
    }

    @Override
    public void saveUserSeq(Long userId, Integer userSeq) {
        redisUtil.setCache(StrUtil.format(USER_SEQ_REDIS_KEY, userId), userSeq);
    }
}
