package com.dusk.module.ddm.module.auth.repository;


import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.Setting;

/**
 * @author kefuming
 * @date 2020-05-21 15:11
 */
public interface ISettingRepository extends IBaseRepository<Setting> {
    Setting findSettingByNameAndTenantId(String name, Long tenantId);
}
