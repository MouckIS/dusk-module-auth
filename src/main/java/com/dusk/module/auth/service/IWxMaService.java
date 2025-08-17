package com.dusk.module.auth.service;

import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.binarywang.wx.miniapp.bean.WxMaUserInfo;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.weixin.WxMaSessionResult;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-03-16 13:37
 */
public interface IWxMaService {
    /**
     * 获取微信登陆session_key，如果曾经用手机号登陆过，回返回登陆参数
     *
     * @param appid
     * @param code
     * @return
     */
    WxMaSessionResult getSession(String appid, String code);

    /**
     * 获取微信用户信息
     *
     * @param appid
     * @param openid
     * @param encryptedData
     * @param iv
     * @return
     */
    WxMaUserInfo info(String appid, String openid, String encryptedData, String iv);

    /**
     * 微信小程序登陆，获取登陆参数
     *
     * @param appid
     * @param openid
     * @param encryptedData
     * @param iv
     * @return
     */
    List<MobileUserDto> login(String appid, String openid, String encryptedData, String iv);

    /**
     * 解密微信用户手机号
     *
     * @param appid         appid
     * @param openid        openid
     * @param encryptedData 被加密数据
     * @param iv            加密算法的初始向量
     * @return
     */
    WxMaPhoneNumberInfo phone(String appid, String openid, String encryptedData, String iv);
}
