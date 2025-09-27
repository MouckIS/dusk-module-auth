package com.dusk.module.ddm.module.auth.repository.dashboard;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.dashboard.DashboardZone;

import java.util.List;

/**
 * @author 简建鸿
 * @date 2021-08-07
 */
public interface IDashBoardZoneRepository extends IBaseRepository<DashboardZone> {
    List<DashboardZone> findAllByClassifyIdIn(List<Long> classifyIds);
    List<DashboardZone> findAllByClassifyIdOrderByZonePosition(Long classifyId);
    void deleteAllByClassifyId(Long classifyId);
}
