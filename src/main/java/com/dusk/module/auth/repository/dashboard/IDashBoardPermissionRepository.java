package com.dusk.module.auth.repository.dashboard;

import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.dashboard.DashboardPermission;

import java.util.List;

/**
 * @author 简建鸿
 * @date 2021-07-26
 */
public interface IDashBoardPermissionRepository extends IBaseRepository<DashboardPermission> {
    void deleteByThemeId(Long themeId);
    void deleteByThemeIdAndRoleId(Long themeId, Long roleId);
    DashboardPermission findByThemeIdAndRoleId(Long themeId, Long roleId);
    List<DashboardPermission> findAllByRoleId(Long roleId);
    List<DashboardPermission> findAllByRoleIdIn(List<Long> roleId);
    List<DashboardPermission> findAllByThemeId(Long themeId);
}
