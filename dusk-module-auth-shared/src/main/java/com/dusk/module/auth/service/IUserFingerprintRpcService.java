package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.fingerprint.GetAllInputDto;
import com.dusk.module.auth.dto.fingerprint.UserFingerprintDto;

import java.util.List;

/**
 * @author panyanlin1
 * @date 2021-05-11 17:10:02
 */
public interface IUserFingerprintRpcService {
    /**
     * 查询指纹记录
     */
    List<UserFingerprintDto> getAll(GetAllInputDto inputDto);
}
