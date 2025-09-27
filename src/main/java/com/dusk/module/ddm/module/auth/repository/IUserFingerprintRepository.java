package com.dusk.module.ddm.module.auth.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.UserFingerprint;

import java.util.List;


/**
 * @author panyanlin1
 * @date 2021-05-11 17:10:02
 */
public interface IUserFingerprintRepository extends IBaseRepository<UserFingerprint> {
    void deleteByIdIn(List<Long> ids);
}
