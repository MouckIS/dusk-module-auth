package com.dusk.module.auth.registry.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.registry.entity.PermissionDefinition;
import com.dusk.module.auth.registry.enums.PermissionStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 业务权限点仓库，见《权限优化方案-整理版》4.3 / 4.8。
 */
public interface IPermissionDefinitionRepository extends IBaseRepository<PermissionDefinition> {

    Optional<PermissionDefinition> findByCode(String code);

    List<PermissionDefinition> findByCodeIn(Collection<String> codes);

    List<PermissionDefinition> findByStatus(PermissionStatus status);
}
