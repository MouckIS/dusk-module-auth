package com.dusk.module.ddm.module.auth.service;

import com.dusk.module.auth.dto.captcha.CaptchaInputDto;
import com.dusk.module.auth.dto.captcha.CaptchaOutDto;

import javax.servlet.http.HttpServletRequest;

/**
 * @author kefuming
 * @date 2020-12-15 14:53
 */
public interface ICaptchaService {

    /**
     * 获取验证码
     *
     * @return
     */
    CaptchaOutDto getCaptcha();

    /**
     * 校验验证码(帐户名密码登陆用)
     *
     * @param loginRequest
     * @param request
     * @return
     */
    boolean verifyCaptcha(CaptchaInputDto loginRequest, HttpServletRequest request);


    /**
     * 校验验证码(手机号登陆 发送手机验证码)
     *
     * @param loginRequest
     * @param request
     * @return
     */
    boolean verifyCaptchaSendMobile(CaptchaInputDto loginRequest, HttpServletRequest request);


    /**
     * 设置发送次数缓存
     * @param request
     */
    void setMobileSendCaptchaCount(HttpServletRequest request);

    /**
     * 记录登陆错误次数，如果返回true，则意味着需要验证码
     *
     * @param request
     * @return
     */
    boolean checkAndWriteError(HttpServletRequest request);


    /**
     * 获取是否需要验证码
     *
     * @param request
     * @return
     */
    boolean checkNeedCaptcha(HttpServletRequest request);


    /**
     * 清除错误次数缓存
     *
     * @param request
     * @return
     */
    void clearBuffer(HttpServletRequest request);
}
