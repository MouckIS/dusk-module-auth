package com.dusk.module.auth.service;

/**
 * 微信小程序相关接口
 *
 * @author kefuming
 * @date 2021-03-16 13:53
 */
public interface IWxMaRpcService {
    /**
     * 解密微信用户手机号(这个接口的目的是用于开放接口 当你无法验证手机号是否是本人的时候依赖微信的机制获取可信任手机号)
     *
     * @param appid         appid
     * @param openid        openid
     * @param encryptedData 被加密数据
     * @param iv            加密算法的初始向量
     * @return
     */
    String getPhoneNumber(String appid, String openid, String encryptedData, String iv);
}
