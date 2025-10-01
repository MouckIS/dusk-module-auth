package com.dusk.module.auth.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.binarywang.wx.miniapp.bean.WxMaUserInfo;
import cn.hutool.core.util.StrUtil;
import com.dusk.commom.rpc.auth.service.IWxMaRpcService;
import com.github.dozermapper.core.Mapper;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.exception.MobileAccountNotFoundException;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.module.auth.common.config.WxMaConfiguration;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.weixin.WxMaSessionResult;
import com.dusk.module.auth.service.IMobileLoginService;
import com.dusk.module.auth.service.IWxMaService;
import com.dusk.module.auth.service.IWxMaTestService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author kefuming
 * @date 2021-03-16 11:46
 */
@Service
@Slf4j
public class WxMaServiceImpl implements IWxMaRpcService, IWxMaService {
    @Autowired
    Mapper dozerMapper;
    @Autowired
    IMobileLoginService mobileLoginService;
    @Autowired(required = false)
    RedisUtil<String> redisUtil;

    @Autowired(required = false)
    IWxMaTestService wxMaTestService;


    private final static String REDIS_WX_MOBILE_CACHE_PREFIX = "CRUX:LOGIN:WX:MOBILE:OPENID:";
    private final static String REDIS_WX_SESSIONKEY_CACHE_PREFIX = "CRUX:LOGIN:WX:SESSIONKEY:OPENID:";
    /**
     * 微信手机号缓存时长 单位天
     */
    private final static int REDIS_WX_MOBILE_CACHE_DATE = 30;
    /**
     * 微信sessionkey缓存 单位分钟 默认10分钟
     */
    private final static int REDIS_WX_SESSIONKEY_CACHE_MINUTE = 10;


    @Override
    public WxMaSessionResult getSession(String appid, String code) {
        final WxMaService wxService = WxMaConfiguration.getMaService(appid);
        try {
            WxMaJscode2SessionResult session = wxService.getUserService().getSessionInfo(code);
            WxMaSessionResult result = dozerMapper.map(session, WxMaSessionResult.class);
            //如果开启了redis缓存，则从缓存读取手机号，并默认获取登陆信息
            if (redisUtil != null) {
                String mobile = redisUtil.getCache(REDIS_WX_MOBILE_CACHE_PREFIX + session.getOpenid());
                if (StrUtil.isNotEmpty(mobile)) {
                    try {
                        result.setLoginData(mobileLoginService.getValidMobileUser(mobile));
                        //微信小程序手机号续期
                        redisUtil.setExpire(REDIS_WX_MOBILE_CACHE_PREFIX + session.getOpenid(), REDIS_WX_MOBILE_CACHE_DATE, TimeUnit.DAYS);
                    } catch (Exception ex) {
                        log.error("获取默认登陆信息失败:{}", ex.getMessage());
                    }

                }
                redisUtil.setCache(REDIS_WX_SESSIONKEY_CACHE_PREFIX + session.getOpenid(), session.getSessionKey(), REDIS_WX_SESSIONKEY_CACHE_MINUTE, TimeUnit.MINUTES);
            } else {
                throw new BusinessException("尚未开启redis缓存配置，无法使用微信小程序相关功能");
            }
            return result;
        } catch (WxErrorException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public WxMaUserInfo info(String appid, String openid, String encryptedData, String iv) {
        final WxMaService wxService = WxMaConfiguration.getMaService(appid);
        String sessionKey = getSessionKey(openid);
        // 解密用户信息
        WxMaUserInfo userInfo = wxService.getUserService().getUserInfo(sessionKey, encryptedData, iv);
        return userInfo;
    }

    @Override
    public List<MobileUserDto> login(String appid, String openid, String encryptedData, String iv) {
        final WxMaService wxService = WxMaConfiguration.getMaService(appid);
        String sessionKey = getSessionKey(openid);
        // 解密
        WxMaPhoneNumberInfo phoneNoInfo = wxService.getUserService().getPhoneNoInfo(sessionKey, encryptedData, iv);

        List<MobileUserDto> validMobileUser = new ArrayList<>();

        try {
            validMobileUser = mobileLoginService.getValidMobileUser(phoneNoInfo.getPhoneNumber());
        } catch (MobileAccountNotFoundException ex) {
            //当手机号不存在任意用户 并且 允许微信小程序审核模式 则 再去拉取下指定的账户
            if (wxMaTestService != null) {
                validMobileUser = wxMaTestService.getValidTestUser();
            } else {
                throw new BusinessException(ex.getMessage());
            }
        }


        //如果开启了redis,并且系统存在账号 就缓存 openid和手机号得绑定关系
        if (redisUtil != null && validMobileUser.size() > 0) {
            redisUtil.setCache(REDIS_WX_MOBILE_CACHE_PREFIX + openid, phoneNoInfo.getPhoneNumber(), REDIS_WX_MOBILE_CACHE_DATE, TimeUnit.DAYS);
        }

        return validMobileUser;
    }

    @Override
    public WxMaPhoneNumberInfo phone(String appid, String openid, String encryptedData, String iv) {
        final WxMaService wxService = WxMaConfiguration.getMaService(appid);
        String sessionKey = getSessionKey(openid);
        WxMaPhoneNumberInfo phoneNoInfo = wxService.getUserService().getPhoneNoInfo(sessionKey, encryptedData, iv);
        return phoneNoInfo;
    }


    /**
     * 获取sessionkey
     *
     * @param openid
     * @return
     */
    private String getSessionKey(String openid) {
        //如果开启了redis缓存，则从缓存读取手机号，并默认获取登陆信息
        if (redisUtil != null) {
            String tempKey = redisUtil.getCache(REDIS_WX_SESSIONKEY_CACHE_PREFIX + openid);
            if (StrUtil.isNotEmpty(tempKey)) {
                return tempKey;
            } else {
                throw new BusinessException("获取用户信息失败");
            }
        } else {
            throw new BusinessException("尚未开启redis缓存配置，无法使用微信小程序相关功能");
        }
    }

    @Override
    public String getPhoneNumber(String appid, String openid, String encryptedData, String iv) {
        WxMaPhoneNumberInfo phoneNoInfo = phone(appid, openid, encryptedData, iv);
        //如果开启了redis, 就缓存 openid和手机号得绑定关系, 域账号登录时也缓存
        if (redisUtil != null && Objects.nonNull(phoneNoInfo) && StrUtil.isNotBlank(phoneNoInfo.getPhoneNumber())) {
            redisUtil.setCache(REDIS_WX_MOBILE_CACHE_PREFIX + openid, phoneNoInfo.getPhoneNumber(), REDIS_WX_MOBILE_CACHE_DATE, TimeUnit.DAYS);
        }
        return phoneNoInfo.getPhoneNumber();
    }
}
