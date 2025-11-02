package com.dusk.module.auth.service.impl;

import com.dusk.common.rpc.auth.dto.fingerprint.GetAllInputDto;
import com.dusk.common.rpc.auth.dto.fingerprint.UserFingerprintDto;
import com.dusk.common.rpc.auth.service.IUserFingerprintRpcService;
import com.dusk.module.auth.service.IUserFingerprintService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-05-13 8:23
 */
@DubboService
public class UserFingerprintRpcServiceImpl implements IUserFingerprintRpcService {
    @Resource
    private IUserFingerprintService userFingerprintService;

    @Override
    public List<UserFingerprintDto> getAll(GetAllInputDto inputDto) {
        return userFingerprintService.getAll(inputDto);
    }
}
