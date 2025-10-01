package com.dusk.module.auth.repository;

import com.dusk.common.core.annotation.DisableGlobalFilter;
import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.GrantPermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-04-29 10:54
 */
public interface IGrantPermissionRepository extends IBaseRepository<GrantPermission> {
    List<GrantPermission> findDistinctByRoleIdIn(Long[] ids);

    List<GrantPermission> findDistinctByRoleId(Long id);

    void deleteAllByRoleId(Long id);

    @EntityGraph("GrantPermission.includeAll")
    @DisableGlobalFilter
    List<GrantPermission> findAll();

    @Query("select gp.role.id from GrantPermission gp where gp.name = :name")
    List<Long> findRoleIdsByPermissionName(String name);
}
