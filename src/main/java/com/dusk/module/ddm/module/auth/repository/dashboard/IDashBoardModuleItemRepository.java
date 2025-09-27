package com.dusk.module.ddm.module.auth.repository.dashboard;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.dashboard.DashboardModuleItem;

import java.util.List;

/**
 * @author 简建鸿
 * @date 2021-07-26
 */
public interface IDashBoardModuleItemRepository extends IBaseRepository<DashboardModuleItem> {
    void deleteByModuleId(Long moduleId);
    List<DashboardModuleItem> findAllByModuleId(Long moduleId);
    List<DashboardModuleItem> findAllByModuleIdOrderByCreateTime(Long moduleId);
    List<DashboardModuleItem> findAllByModuleIdAndName(Long moduleId, String name);
}
