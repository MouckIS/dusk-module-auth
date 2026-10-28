package com.dusk.module.auth.repository;


import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.Tenant;

import java.util.List;
import java.util.Optional;

public interface ITenantRepository extends IBaseRepository<Tenant> {
    Optional<Tenant> findByTenantName(String tenantName);

    /**
     * 套餐变更时需要找到所有受影响租户，进而推进其角色的 permissionVersion（3.3）。
     */
    List<Tenant> findByEdition_Id(Long editionId);
}
