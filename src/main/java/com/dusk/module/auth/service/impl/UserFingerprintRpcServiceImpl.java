package com.dusk.module.auth.service.impl;

import com.dusk.commom.rpc.auth.dto.fingerprint.GetAllInputDto;
import com.dusk.commom.rpc.auth.dto.fingerprint.UserFingerprintDto;
import com.dusk.commom.rpc.auth.service.IUserFingerprintRpcService;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.module.auth.service.IUserFingerprintService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-05-13 8:23
 */
@Service
public class UserFingerprintRpcServiceImpl implements IUserFingerprintRpcService {
    @Autowired
    IUserFingerprintService userFingerprintService;

    @Override
    public List<UserFingerprintDto> getAll(GetAllInputDto inputDto) {
        return userFingerprintService.getAll(inputDto);
    }
}
