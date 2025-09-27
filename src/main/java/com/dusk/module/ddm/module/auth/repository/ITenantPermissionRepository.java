package com.dusk.module.ddm.module.auth.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.TenantPermission;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author kefuming
 * @date 2020-12-11 9:11
 */
public interface ITenantPermissionRepository extends IBaseRepository<TenantPermission> {
    @Transactional
    void deleteByEditionId(Long editionId);
}
