package com.dusk.module.auth.service;

import com.dusk.common.framework.auth.permission.RoleInfo;
import com.dusk.common.framework.service.IBaseService;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.repository.IGrantPermissionRepository;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2021-08-27 14:02
 */
public interface IGrantPermissionService extends IBaseService<GrantPermission, IGrantPermissionRepository> {
    /**
     * 获取所有的授权权限map
     *
     * @return
     */
    Map<String, List<RoleInfo>> getAll();


    /**
     * 添加动态权限
     * @param name
     * @param roleIds
     * @param businessKey
     */
    void addDynamicPermission(List<String> name, List<Long> roleIds, String businessKey);

    void deleteDynamicPermission(String businessKey);
}
