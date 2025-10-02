package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dusk.common.mqs.pusher.PushSMS;
import com.dusk.common.mqs.pusher.SmsPushConfig;
import com.dusk.common.mqs.pusher.SmsTemplateParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dozermapper.core.Mapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.annotation.DisableTenantFilter;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.exception.MobileAccountNotFoundException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.util.LoginUtils;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.mobilelogin.SendCaptchaInput;
import com.dusk.module.auth.entity.QUser;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.feature.UserFeatureProvider;
import com.dusk.module.auth.push.INotificationPushManager;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ICaptchaService;
import com.dusk.module.auth.service.IFeatureService;
import com.dusk.module.auth.service.IMobileLoginService;
import com.dusk.module.auth.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pengmengjiang
 * @date 2020/10/14 11:29
 */
@Service
@Slf4j
public class MobileLoginServiceImpl implements IMobileLoginService {
    private final static String REDIS_KEY_MOBILE_LOGIN_KEY_PREFIX = "CRUX:MOBILE:LOGIN:";
    private final static String REDIS_KEY_MOBILE_SEND_KEY_PREFIX = "CRUX:MOBILE:SEND:";
    //缓存手机验证码时长 单位 秒 默认给5分钟 可以改
    private final static int MOBILE_CAPTCHA_TIME = 60 * 5;

    @Autowired(required = false)
    RedisUtil<String> redisUtil;
    @Autowired
    private TokenAuthManager tokenAuthManager;
    @Autowired
    JPAQueryFactory queryFactory;
    @Autowired
    SmsPushConfig smsPushConfig;
    @Autowired
    ObjectMapper jsonMapper;
    @Autowired
    Mapper dozerMapper;
    @Autowired(required = false)
    INotificationPushManager pushManager;
    @Autowired
    private IFeatureService featureService;
    @Autowired
    IUserRepository userRepository;
    @Autowired
    IUserService userService;
    @Autowired
    ICaptchaService captchaService;

    @Override
    @DisableTenantFilter
    public void captcha(SendCaptchaInput input, HttpServletRequest request) {
        if (pushManager == null) {
            throw new BusinessException("消息推送服务未启用");
        }

        if (TenantContextHolder.getTenantId() != null && !Boolean.parseBoolean(featureService.getFeatureValue(TenantContextHolder.getTenantId(), UserFeatureProvider.APP_USER_ALLOW_MOBILE_LOGIN))) {
            throw new BusinessException("当前系统不允许手机登录");
        }
        captchaService.verifyCaptchaSendMobile(input, request);

        QUser user = QUser.user;
        var query = queryFactory.select(user).from(user)
                .where(user.phoneNo.eq(input.getMobile()));
        if (TenantContextHolder.getTenantId() != null) {
            query = queryFactory.select(user).from(user)
                    .where(user.phoneNo.eq(input.getMobile()), user.tenantId.eq(TenantContextHolder.getTenantId()));
        }
        if (query.fetchCount() == 0) {
            throw new BusinessException("系统无此手机号账户");
        }

        //后端强制校验每个手机号1分钟只能发送一个验证码
        if (StrUtil.isNotEmpty(redisUtil.getCache(REDIS_KEY_MOBILE_SEND_KEY_PREFIX + input.getMobile()))) {
            throw new BusinessException("发送频率过快");
        }

        String captcha = RandomStringUtils.randomNumeric(6);
        sendCaptchaSms(input.getMobile(), captcha);
        redisUtil.setCache(REDIS_KEY_MOBILE_LOGIN_KEY_PREFIX + input.getMobile(), captcha, MOBILE_CAPTCHA_TIME); //5分钟内有效
        //缓存发送手机号 1分钟
        redisUtil.setCache(REDIS_KEY_MOBILE_SEND_KEY_PREFIX + input.getMobile(), "1", 60);

        captchaService.setMobileSendCaptchaCount(request);
    }

    @Override
    @DisableTenantFilter
    public List<MobileUserDto> login(String mobile, String captcha) {
        String cachedCaptcha = redisUtil.getCache(REDIS_KEY_MOBILE_LOGIN_KEY_PREFIX + mobile);
        if (StringUtils.isBlank(cachedCaptcha) || !StringUtils.equals(captcha, cachedCaptcha)) {
            throw new BusinessException("验证码错误");
        }
        redisUtil.deleteCache(REDIS_KEY_MOBILE_LOGIN_KEY_PREFIX + mobile);
        return getValidMobileUser(mobile);
    }

    @Override
    @DisableTenantFilter
    public List<MobileUserDto> getValidMobileUser(String mobile) {
        List<MobileUserDto> list = new ArrayList<>();
        List<User> userList;
        if (TenantContextHolder.getTenantId() == null) {
            userList = userRepository.findByPhoneNo(mobile);
        } else {
            userList = userRepository.findByPhoneNoAndTenantId(mobile, TenantContextHolder.getTenantId());
        }

        //过滤租户 开放手机号登陆的
        List<User> realList = new ArrayList<>();
        for (User temp : userList) {
            if (temp.getTenantId() != null && !Boolean.parseBoolean(featureService.getFeatureValue(temp.getTenantId(), UserFeatureProvider.APP_USER_ALLOW_MOBILE_LOGIN))) {
                continue;
            }
            realList.add(temp);
        }
        for (User temp : realList) {
            try {
                userService.checkUserValid(temp);
                UserContext context = LoginUtils.getUserContextByUser(temp);
                String token = tokenAuthManager.generateToken(context);
                MobileUserDto dto = dozerMapper.map(temp, MobileUserDto.class);
                dto.setToken(token);
                list.add(dto);

            } catch (Exception ex) {
                log.info("检测登陆用户异常：{}", ex.getMessage());
                if (realList.size() == 1) {
                    throw new BusinessException(ex.getMessage());
                }
            }

        }
        if (list.size() == 0) {
            throw new MobileAccountNotFoundException("系统无此手机号账户");
        }
        return list;
    }

    @SneakyThrows
    void sendCaptchaSms(String mobile, String captcha) {
        PushSMS sms = new PushSMS();
        sms.setPhoneNumbers(mobile);
        sms.setSignName(smsPushConfig.getSmsSignName());
        sms.setTemplateCode(smsPushConfig.getSmsVerificationCode());
        SmsTemplateParam[] params = new SmsTemplateParam[1];
        SmsTemplateParam param = new SmsTemplateParam();
        param.setCode("code");
        param.setProduct(captcha);
        params[0] = param;
        sms.setTemplateParams(params);
        pushManager.smsPushAsync(sms);
    }
}
