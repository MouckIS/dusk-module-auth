package com.dusk.module.auth.service;

import com.dusk.module.auth.dto.token.TokenSign;

/**
 * @author : caiwenjun
 * @date : 2023/5/31 10:30
 */
public interface ITokenService {
    /**
     * 长久Token签发
     * @author caiwenjun
     * @date 2023/5/31 10:29
     * @param tokenSign
     * @return String
     */
    String foreverTokenSign(TokenSign tokenSign);
}
