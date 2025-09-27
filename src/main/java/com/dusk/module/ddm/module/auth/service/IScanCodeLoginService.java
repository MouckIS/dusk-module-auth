package com.dusk.module.ddm.module.auth.service;

import com.dusk.common.core.response.BaseApiResult;

/**
 * @author kefuming
 * @date 2021-04-29 15:06
 */
public interface IScanCodeLoginService {
    String getLoginKey();

    BaseApiResult<String> getToken(String key);

    void login(String key);
}
