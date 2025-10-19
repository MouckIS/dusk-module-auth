package com.dusk.module.auth.service.impl;

import com.dusk.common.rpc.auth.dto.GenerateTokenForNonUserInput;
import com.dusk.common.rpc.auth.service.ITokenAuthRpcService;
import com.dusk.module.auth.dto.token.TokenSign;
import com.dusk.module.auth.service.ITokenService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author : caiwenjun
 * @date : 2023/5/31 10:30
 */
@Component
@Slf4j
public class TokenServiceImpl implements ITokenService {

    @Resource
    private ITokenAuthRpcService tokenAuthRpcService;

    @Override
    public String foreverTokenSign(TokenSign tokenSign) {
        GenerateTokenForNonUserInput input = new GenerateTokenForNonUserInput();
        BeanUtils.copyProperties(tokenSign, input);
        input.setUnit(TimeUnit.DAYS);
        return tokenAuthRpcService.generateTokenForNonUser(input);
    }
}
