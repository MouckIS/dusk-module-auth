package com.dusk.module.auth.authz.version;

import com.dusk.common.core.repository.IBaseRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 角色权限版本仓储。
 */
public interface IAuthzRoleVersionRepository extends IBaseRepository<AuthzRoleVersion> {

    Optional<AuthzRoleVersion> findByRoleId(Long roleId);

    /**
     * 加行级悲观锁读取，保证同一角色并发的权限变更串行化，版本号不会重复或倒退。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from AuthzRoleVersion v where v.roleId = :roleId")
    Optional<AuthzRoleVersion> findWithLockByRoleId(@Param("roleId") Long roleId);
}
