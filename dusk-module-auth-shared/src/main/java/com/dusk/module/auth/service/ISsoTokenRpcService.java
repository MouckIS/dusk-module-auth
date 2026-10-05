package com.dusk.module.auth.service;

/**
 * @author : kefuming
 * @date : 2025/8/17 14:24
 */
public interface ISsoTokenRpcService {
    String ssoSm4Token(String encryptStr);
}
