package com.dusk.module.ddm.module.auth.service;

import com.dusk.common.core.service.IBaseService;
import com.dusk.common.module.auth.dto.fingerprint.GetAllInputDto;
import com.dusk.common.module.auth.dto.fingerprint.UserFingerprintDto;
import com.dusk.module.auth.dto.fingerprint.IdentifyInputDto;
import com.dusk.module.auth.dto.fingerprint.RegisterFingerprintInputDto;
import com.dusk.module.auth.dto.fingerprint.SaveFingerprintInputDto;
import com.dusk.module.auth.entity.UserFingerprint;
import com.dusk.module.auth.repository.IUserFingerprintRepository;

import java.util.List;

;

/**
 * @author panyanlin1
 * @date 2021-05-11 17:10:02
 */
public interface IUserFingerprintService extends IBaseService<UserFingerprint, IUserFingerprintRepository> {
    /**
     * 发送注册指纹指令给指纹采集器
     */
    void registerFingerprint(RegisterFingerprintInputDto inputDto);

    /**
     * 保存/更新指纹信息
     */
    Long saveFingerprint(SaveFingerprintInputDto inputDto);

    /**
     * 删除指纹记录
     */
    void deleteByIds(List<Long> ids);

    /**
     * 查询指纹记录
     */
    List<UserFingerprintDto> getAll(GetAllInputDto inputDto);

    /**
     * 发送验证用户指纹指令给指纹采集器
     */
    void identify(IdentifyInputDto inputDto);

    Long saveFingerprintPrivate(SaveFingerprintInputDto inputDto);

    void deleteByIdsPrivate(List<Long> ids);
}
