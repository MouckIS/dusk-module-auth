package com.dusk.module.auth.authz.version;

import com.dusk.common.core.repository.IBaseRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 用户代际版本仓储。
 */
public interface IAuthzSubjectVersionRepository extends IBaseRepository<AuthzSubjectVersion> {

    Optional<AuthzSubjectVersion> findByTenantScopeAndUserId(String tenantScope, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from AuthzSubjectVersion v where v.tenantScope = :tenantScope and v.userId = :userId")
    Optional<AuthzSubjectVersion> findWithLockByScopeAndUserId(@Param("tenantScope") String tenantScope,
                                                               @Param("userId") Long userId);
}
