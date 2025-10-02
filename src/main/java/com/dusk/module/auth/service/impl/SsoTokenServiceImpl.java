package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.module.auth.common.config.AppAuthConfig;
import com.dusk.module.auth.service.ISsoTokenService;
import com.dusk.module.auth.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author kefuming
 * @date 2021-11-25 17:14
 */
@Service
public class SsoTokenServiceImpl implements ISsoTokenService {


    @Autowired
    IUserService userService;
    @Autowired
    AppAuthConfig appAuthConfig;

    @Autowired(required = false)
    RedisUtil<String> redisUtil;

    @Value("${sso.time-diff}")
    int timeDiff;

    private static final String SSO_KEY = "CRUX:AUTH:SSO:SM4:{}";

    @Override
    public String ssoSm4Token(String encryptStr) {

        //当redis启用情况下  做票据只允许使用一次的机制 防止重放
        if (redisUtil != null && redisUtil.hasKey(StrUtil.format(SSO_KEY, encryptStr))) {
            throw new BusinessException("非法请求");
        }
        String userName;
        long timeStamp;
        try {
            String decryptStr = SmUtil.sm4(HexUtil.decodeHex(appAuthConfig.getLoginEncryptKey())).decryptStr(encryptStr, CharsetUtil.CHARSET_UTF_8);
            String[] arrays = decryptStr.split("\\|");
            userName = arrays[0];
            timeStamp = Long.parseLong(arrays[1]);
        } catch (Exception ex) {
            throw new BusinessException("非法请求");
        }
        int min = timeDiff;
        int expire = min * 60;
        long oldTimestamp = System.currentTimeMillis();
        if (Math.abs(oldTimestamp - timeStamp) / 1000 <= expire) {
            if (redisUtil != null) {
                redisUtil.setCache(StrUtil.format(SSO_KEY, encryptStr), "", min, TimeUnit.MINUTES);
            }
            return userService.generateTokenByUserName(userName);
        } else {
            throw new BusinessException("请求已过期,有效时间" + min + "分钟");
        }

    }
}
