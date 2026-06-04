package com.dusk.module.auth.dimension.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.dimension.entity.DataDimension;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 数据维度仓储
 *
 * @author dusk
 */
@Repository
public interface IDataDimensionRepository extends IBaseRepository<DataDimension> {

    /**
     * 根据维度编码查找
     *
     * @param dimensionCode 维度编码
     * @return 数据维度
     */
    Optional<DataDimension> findByDimensionCode(String dimensionCode);

    /**
     * 判断维度编码是否已存在
     *
     * @param dimensionCode 维度编码
     * @return 是否存在
     */
    boolean existsByDimensionCode(String dimensionCode);
}
