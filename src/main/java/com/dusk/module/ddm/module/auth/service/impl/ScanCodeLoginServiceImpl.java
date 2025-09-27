package com.dusk.module.ddm.module.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.response.BaseApiResult;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.service.IScanCodeLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author kefuming
 * @date 2021-04-29 15:08
 */
@Service
public class ScanCodeLoginServiceImpl implements IScanCodeLoginService {
    @Autowired(required = false)
    RedisUtil<String> redisUtil;

    @Autowired
    SecurityUtils securityUtils;

    @Autowired
    TokenAuthManager tokenAuthManager;

    private static final String SCAN_CODE_LOGIN_KEY = "CRUX:LOGIN:SCANCODE:";
    //缓存5分钟
    private static int LOGIN_KEY_TIME = 5;

    @Override
    public String getLoginKey() {
        String id = IdUtil.fastSimpleUUID();
        if (redisUtil == null) {
            throw new BusinessException("服务端尚未开启缓存服务，无法使用扫码登陆");
        }
        redisUtil.setCache(SCAN_CODE_LOGIN_KEY + id, "", LOGIN_KEY_TIME, TimeUnit.MINUTES);
        return id;
    }

    @Override
    public BaseApiResult<String> getToken(String key) {
        if (redisUtil == null) {
            throw new BusinessException("服务端尚未开启缓存服务，无法使用扫码登陆");
        }
        String realKey = SCAN_CODE_LOGIN_KEY + key;
        String cache = redisUtil.getCache(realKey);
        BaseApiResult<String> result = BaseApiResult.success(cache, BaseApiResult.MESSAGE_SUCCESS);
        if (cache == null) {
            result.setCode(1001);
        }
        if (StrUtil.isNotEmpty(cache)) {
            redisUtil.deleteCache(realKey);
        }
        return result;
    }

    @Override
    public void login(String key) {
        if (redisUtil == null) {
            throw new BusinessException("服务端尚未开启缓存服务，无法使用扫码登陆");
        }
        UserContext currentUser = securityUtils.getCurrentUser();
        String token = tokenAuthManager.generateToken(currentUser, 365, TimeUnit.DAYS);
        String realKey = SCAN_CODE_LOGIN_KEY + key;
        redisUtil.setCache(realKey, token, LOGIN_KEY_TIME, TimeUnit.MINUTES);
    }
}
