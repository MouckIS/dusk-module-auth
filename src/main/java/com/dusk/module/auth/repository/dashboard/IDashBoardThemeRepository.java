package com.dusk.module.auth.repository.dashboard;

import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.dashboard.DashboardTheme;

import java.util.List;

/**
 * @author 简建鸿
 * @date 2021-07-26
 */
public interface IDashBoardThemeRepository extends IBaseRepository<DashboardTheme> {
    List<DashboardTheme> findAllByIdIn(List<Long> ids);
    DashboardTheme findFirstByMainPage(Boolean mainPage);
    List<DashboardTheme> findAllByName(String name);
}
