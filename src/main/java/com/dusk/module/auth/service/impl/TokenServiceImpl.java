package com.dusk.module.auth.service.impl;

import com.dusk.commom.rpc.auth.dto.GenerateTokenForNonUserInput;
import com.dusk.commom.rpc.auth.service.ISmRpcUtil;
import com.dusk.commom.rpc.auth.service.ITokenAuthRpcService;
import com.github.dozermapper.core.Mapper;
import lombok.extern.slf4j.Slf4j;
import com.dusk.module.auth.dto.token.TokenSign;
import com.dusk.module.auth.service.ITokenService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
    @Resource
    private ISmRpcUtil smRpcUtil;
    @Resource
    private Mapper dozerMapper;

    @Override
    public String foreverTokenSign(TokenSign tokenSign) {
        GenerateTokenForNonUserInput input = dozerMapper.map(tokenSign, GenerateTokenForNonUserInput.class);
        input.setUnit(TimeUnit.DAYS);
        return tokenAuthRpcService.generateTokenForNonUser(input);
    }
}
