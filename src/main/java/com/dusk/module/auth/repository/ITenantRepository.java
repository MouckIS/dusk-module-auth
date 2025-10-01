package com.dusk.module.auth.repository;


import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.Tenant;

import java.util.Optional;

public interface ITenantRepository extends IBaseRepository<Tenant> {
    Optional<Tenant> findByTenantName(String tenantName);
}
