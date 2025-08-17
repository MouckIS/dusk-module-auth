package com.dusk.module.auth.service;

import com.dusk.common.framework.service.IBaseService;
import com.dusk.common.module.auth.dto.role.RolePermissionDto;
import com.dusk.module.auth.dto.permission.EditionPermissionInputDto;
import com.dusk.module.auth.entity.TenantPermission;
import com.dusk.module.auth.repository.ITenantPermissionRepository;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-12-11 9:11
 */
public interface ITenantPermissionService extends IBaseService<TenantPermission, ITenantPermissionRepository> {

    /**
     * 根据版本id获取权限清单
     *
     * @param editionId
     * @return
     */
    List<RolePermissionDto> getEditionPermissions(Long editionId);

    /**
     * 设置版本的权限清单
     *
     * @param input
     */
    void setEditionPermissions(EditionPermissionInputDto input);


    /**
     * 根据租户id获取权限清单
     *
     * @param tenantId
     * @return
     */
    List<RolePermissionDto> getTenantPermissions(Long tenantId);


    /**
     * 获取租户有权限的清单
     *
     * @param tenantId
     * @return
     */
    List<String> getGrantedPermissionByTenantId(Long tenantId);


    /**
     * 获取所有的租户权限清单
     *
     * @return
     */
    Map<String, List<Long>> getAllTenantPermission();
}
