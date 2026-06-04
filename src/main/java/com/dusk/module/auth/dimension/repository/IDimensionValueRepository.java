package com.dusk.module.auth.dimension.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.dimension.entity.DimensionValue;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 维度值仓储
 *
 * @author dusk
 */
@Repository
public interface IDimensionValueRepository extends IBaseRepository<DimensionValue> {

    /**
     * 根据维度ID查找所有维度值
     *
     * @param dimensionId 维度ID
     * @return 维度值列表
     */
    List<DimensionValue> findByDimensionIdOrderBySortIndexAsc(Long dimensionId);

    /**
     * 根据维度ID和维度值编码查找
     *
     * @param dimensionId 维度ID
     * @param valueCode   维度值编码
     * @return 维度值列表
     */
    List<DimensionValue> findByDimensionIdAndValueCode(Long dimensionId, String valueCode);

    /**
     * 根据维度ID删除所有维度值
     *
     * @param dimensionId 维度ID
     */
    void deleteByDimensionId(Long dimensionId);

    /**
     * 判断维度下是否存在某个值编码
     *
     * @param dimensionId 维度ID
     * @param valueCode   维度值编码
     * @return 是否存在
     */
    boolean existsByDimensionIdAndValueCode(Long dimensionId, String valueCode);
}
