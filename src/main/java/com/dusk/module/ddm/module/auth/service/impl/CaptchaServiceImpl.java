package com.dusk.module.ddm.module.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.wf.captcha.SpecCaptcha;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.feature.IFeatureChecker;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.response.BaseApiResult;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.dto.captcha.CaptchaInputDto;
import com.dusk.module.auth.dto.captcha.CaptchaOutDto;
import com.dusk.module.auth.feature.LoginFeatureProvider;
import com.dusk.module.auth.service.ICaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * @author kefuming
 * @date 2020-12-15 14:56
 */
@Service
@Slf4j
public class CaptchaServiceImpl implements ICaptchaService {
    @Autowired
    RedisUtil<Object> redisUtil;
    @Autowired
    IFeatureChecker featureChecker;

    //region  帐户名密码登陆相关参数
    private final static String REDIS_KEY_CAPTCHA_PREFIX = "CRUX:LOGIN:CAPTCHA:KEY:";
    private final static String REDIS_KEY_NEED_CAPTCHA_PREFIX = "CRUX:LOGIN:CAPTCHA:IP:";
    //记录ip访问错误次数
    private final static String REDIS_KEY_CAPTCHA_ERROR_COUNT_PREFIX = "CRUX:LOGIN:CAPTCHA:ERROR:IP:";
    //登陆错误次数默认3次 显示验证码
    private final static int MAX_LOGIN_ERROR_COUNT = 3;
    //N分钟内连续错误次数缓存时长
    private final static int ERROR_COUNT_MAX_DELAY = 2;
    //ip显示验证码保留N小时
    private final static int IP_NEED_CAPTCHA_TIME = 1;
    //endregion

    //region 手机登陆验证码相关
    private final static String REDIS_KEY_IP_SEND_KEY_PREFIX = "CRUX:IP:SEND:";
    //独立ip每分钟内最多发送30条，超过则需要验证码
    private final int IP_MAX_SEND_COUNT = 30;
    //endregion

    @Override
    public CaptchaOutDto getCaptcha() {
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 5);
        String verCode = specCaptcha.text().toLowerCase();
        String id = IdUtil.simpleUUID();
        //缓存5分钟
        redisUtil.setCache(REDIS_KEY_CAPTCHA_PREFIX + id, verCode, 5, TimeUnit.MINUTES);
        CaptchaOutDto dto = new CaptchaOutDto();
        dto.setKey(id);
        dto.setImageBase64(specCaptcha.toBase64());
        return dto;
    }

    @Override
    public boolean verifyCaptcha(CaptchaInputDto loginRequest, HttpServletRequest request) {
        Object cache = redisUtil.getCache(REDIS_KEY_NEED_CAPTCHA_PREFIX + ServletUtil.getClientIP(request));
        if (cache != null) {
            return verifyCaptcha(loginRequest);
        } else {
            return true;
        }
    }

    @Override
    public boolean verifyCaptchaSendMobile(CaptchaInputDto loginRequest, HttpServletRequest request) {
        Object cache = redisUtil.getCache(REDIS_KEY_IP_SEND_KEY_PREFIX + ServletUtil.getClientIP(request));
        if (cache != null) {
            int sendCount = Integer.parseInt(cache.toString());
            if (sendCount > IP_MAX_SEND_COUNT) {
                boolean result = verifyCaptcha(loginRequest);
                if (!result) {
                    BaseApiResult apiResult = new BaseApiResult();
                    apiResult.setSuccess(true);
                    apiResult.setCode(1004);
                    apiResult.setMessage("验证码错误");
                    throw new BusinessException(apiResult);
                }
            }
        }
        return true;
    }

    @Override
    public void setMobileSendCaptchaCount(HttpServletRequest request) {
        String key = REDIS_KEY_IP_SEND_KEY_PREFIX + ServletUtil.getClientIP(request);
        redisUtil.increment(key, 1);
        redisUtil.setExpire(key, 60, TimeUnit.SECONDS);
    }


    private boolean verifyCaptcha(CaptchaInputDto loginRequest) {
        String key = loginRequest.getKey();
        String captcha = loginRequest.getCaptcha();
        if (StrUtil.isNotEmpty(key) && StrUtil.isNotEmpty(captcha)) {
            Object compare = redisUtil.getCache(REDIS_KEY_CAPTCHA_PREFIX + key);
            if (compare != null) {
                redisUtil.deleteCache(REDIS_KEY_CAPTCHA_PREFIX + key);
                return StrUtil.equals(compare.toString(), captcha, true);
            }

        }
        return false;
    }

    @Override
    public boolean checkAndWriteError(HttpServletRequest request) {
        String clientIP = ServletUtil.getClientIP(request);
        log.info("帐户名或者密码输入异常，进入验证码流程,ip:{},", clientIP);
        String needKey = REDIS_KEY_NEED_CAPTCHA_PREFIX + clientIP;
        //首先查看下ip是否需要验证码
        Object need = redisUtil.getCache(needKey);
        if (need != null) {
            redisUtil.setCache(needKey, need, IP_NEED_CAPTCHA_TIME, TimeUnit.HOURS);
            return true;
        } else {
            String countKey = REDIS_KEY_CAPTCHA_ERROR_COUNT_PREFIX + clientIP;
            long increment = redisUtil.increment(countKey, 1);
            redisUtil.setExpire(countKey, ERROR_COUNT_MAX_DELAY, TimeUnit.MINUTES);

            int maxCount = MAX_LOGIN_ERROR_COUNT;
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                maxCount = Integer.parseInt(featureChecker.getValue(LoginFeatureProvider.APP_LOGIN_MAX_ERROR));
            }
            if (increment >= maxCount) {
                redisUtil.setCache(needKey, "1", IP_NEED_CAPTCHA_TIME, TimeUnit.HOURS);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean checkNeedCaptcha(HttpServletRequest request) {
        String needKey = REDIS_KEY_NEED_CAPTCHA_PREFIX + ServletUtil.getClientIP(request);
        Object need = redisUtil.getCache(needKey);
        return need != null;
    }

    @Override
    public void clearBuffer(HttpServletRequest request) {
        String countKey = REDIS_KEY_CAPTCHA_ERROR_COUNT_PREFIX + ServletUtil.getClientIP(request);
        redisUtil.deleteCache(countKey);
        String needKey = REDIS_KEY_NEED_CAPTCHA_PREFIX + ServletUtil.getClientIP(request);
        redisUtil.deleteCache(needKey);
    }
}
