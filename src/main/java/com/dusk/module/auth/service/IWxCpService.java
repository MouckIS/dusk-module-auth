package com.dusk.module.auth.service;

import com.dusk.module.auth.dto.weixin.WxCpUserAuthorizationResult;

/**
 * @author jianjianhong
 * @date 2023-10-11 13:37
 */
public interface IWxCpService {

    /**
     * 企业微信服务商应用验证URL
     * @param msg_signature
     * @param timestamp
     * @param nonce
     * @param echostr
     * @return
     */
    String serviceProviderUrlGet(String msg_signature, Integer timestamp, String nonce, String echostr);

    String serviceProviderUrlPost(String msg_signature, String timestamp, String nonce, String body);

    /**
     * 企业微信用户身份验证
     * @param code
     * @param state
     * @param n 租户名称
     * @return
     */
    WxCpUserAuthorizationResult authorization(String code, String state, String n);

    /**
     * 绑定企业微信用户
     * @param wxUserId
     * @param userName
     * @param password
     * @return
     */
    WxCpUserAuthorizationResult bind(String wxUserId, String userName, String password);

}
