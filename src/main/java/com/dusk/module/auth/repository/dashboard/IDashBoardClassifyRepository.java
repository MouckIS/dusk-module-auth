package com.dusk.module.auth.repository.dashboard;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.dashboard.DashboardClassify;

import java.util.List;

/**
 * @author 简建鸿
 * @date 2021-07-26
 */
public interface IDashBoardClassifyRepository extends IBaseRepository<DashboardClassify> {
    List<DashboardClassify> findAllByThemeId(Long themeId);
    List<DashboardClassify> findAllByThemeIdIn(List<Long> themeIds);
    void deleteAllByThemeId(Long themeId);
}
