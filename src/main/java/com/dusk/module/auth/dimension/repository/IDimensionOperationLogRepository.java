package com.dusk.module.auth.dimension.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.dimension.entity.DimensionOperationLog;
import org.springframework.stereotype.Repository;

/**
 * 维度操作日志仓储
 *
 * @author dusk
 */
@Repository
public interface IDimensionOperationLogRepository extends IBaseRepository<DimensionOperationLog> {
}
