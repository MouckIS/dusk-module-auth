package com.dusk.module.auth.repository;


import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.CommonSetting;

import java.util.Optional;

/**
 * @author kefuming
 * @date 2020-05-18 10:52
 */
public interface ICommonSettingRepository extends IBaseRepository<CommonSetting> {
    Optional<CommonSetting> findByKey(String key);
}
