package com.dusk.module.auth.repository.dashboard;

import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.dashboard.DashboardModule;

/**
 * @author 简建鸿
 * @date 2021-07-26
 */
public interface IDashBoardModuleRepository extends IBaseRepository<DashboardModule> {
    DashboardModule findByName(String name);
}
