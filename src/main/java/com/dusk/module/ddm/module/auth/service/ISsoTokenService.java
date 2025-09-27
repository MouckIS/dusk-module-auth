package com.dusk.module.ddm.module.auth.service;

/**
 * @author kefuming
 * @date 2021-11-25 17:12
 */
public interface ISsoTokenService {

    /**
     * 国密4加密快速签发token
     * @param encryptStr
     * @return
     */
    String ssoSm4Token(String encryptStr);
}
