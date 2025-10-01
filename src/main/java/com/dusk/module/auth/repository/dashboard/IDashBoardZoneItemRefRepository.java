package com.dusk.module.auth.repository.dashboard;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.dashboard.DashboardZoneItemRef;

import java.util.List;

/**
 * @author 简建鸿
 * @date 2021-08-07
 */
public interface IDashBoardZoneItemRefRepository extends IBaseRepository<DashboardZoneItemRef> {
    List<DashboardZoneItemRef> findAllByModuleId(Long moduleId);
    List<DashboardZoneItemRef> findAllByModuleItemId(Long moduleItemId);
    List<DashboardZoneItemRef> findAllByZoneId(Long zoneId);
    void deleteAllByZoneIdIn(List<Long> zoneIds);
    void deleteAllByZoneId(Long zoneId);
}
