package com.dusk.module.ddm.module.auth.service.impl;

import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.module.auth.dto.fingerprint.GetAllInputDto;
import com.dusk.common.module.auth.dto.fingerprint.UserFingerprintDto;
import com.dusk.common.module.auth.service.IUserFingerprintRpcService;
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
