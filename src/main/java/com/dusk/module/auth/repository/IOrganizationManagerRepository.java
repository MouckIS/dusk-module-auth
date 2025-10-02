package com.dusk.module.auth.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.OrganizationManager;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;

public interface IOrganizationManagerRepository extends IBaseRepository<OrganizationManager> {
    /**
     * 判断组织id列表是否存在
     * @param orgIds 组织列表
     */
    List<OrganizationManager> findByOrgIdIn(Collection<Long> orgIds);

    /**
     * 根据组织id删除组织管理关系
     */
    @Modifying
    void deleteByOrgId(Long orgId);

    /**
     * 根据userId 删除组织管理关系
     */
    @Modifying
    void deleteByUserId(Long userId);

    /**
     * 批量删除组织管理关系
     */
    void deleteByUserIdIn(Collection<Long> userIds);
}
