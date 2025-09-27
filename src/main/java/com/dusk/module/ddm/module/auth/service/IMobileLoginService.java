package com.dusk.module.ddm.module.auth.service;

import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.mobilelogin.SendCaptchaInput;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author pengmengjiang
 * @date 2020/10/14 11:19
 */
public interface IMobileLoginService {
    /**
     * 获取手机验证码
     *
     * @param input 手机号
     * @return
     */
    void captcha(SendCaptchaInput input, HttpServletRequest request);

    /**
     * 手机号验证码登陆
     *
     * @param mobile
     * @param captcha
     * @return
     */
    List<MobileUserDto> login(String mobile, String captcha);


    /**
     * 手机号登陆唯一入口
     *
     * @param mobile
     * @return
     */
    List<MobileUserDto> getValidMobileUser(String mobile);
}
