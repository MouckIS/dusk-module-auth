package com.dusk.module.auth.dimension.service;

import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.dimension.dto.*;
import com.dusk.module.auth.dimension.entity.DimensionValue;
import com.dusk.module.auth.dimension.repository.IDimensionValueRepository;

import java.util.List;

/**
 * 维度值服务接口
 *
 * @author dusk
 */
public interface IDimensionValueService extends IBaseService<DimensionValue, IDimensionValueRepository> {

    /**
     * 根据维度ID获取维度值列表
     *
     * @param dimensionId 维度ID
     * @return 维度值列表
     */
    List<DimensionValueDto> getByDimensionId(Long dimensionId);

    /**
     * 根据维度编码获取维度值列表
     *
     * @param dimensionCode 维度编码
     * @return 维度值列表
     */
    List<DimensionValueDto> getByDimensionCode(String dimensionCode);

    /**
     * 根据ID获取维度值
     *
     * @param id 维度值ID
     * @return 维度值DTO
     */
    DimensionValueDto getById(Long id);

    /**
     * 创建维度值
     *
     * @param dto 创建请求
     * @return 创建后的维度值DTO
     */
    DimensionValueDto create(DimensionValueCreateDto dto);

    /**
     * 更新维度值
     *
     * @param dto 更新请求
     * @return 更新后的维度值DTO
     */
    DimensionValueDto update(DimensionValueUpdateDto dto);

    /**
     * 删除维度值
     *
     * @param id 维度值ID
     */
    void deleteById(Long id);

    /**
     * 校验维度值
     *
     * @param dto 校验请求
     * @return 校验结果
     */
    DimensionValueValidateResultDto validate(DimensionValueValidateDto dto);

    /**
     * 根据用户权限获取可访问的维度值列表
     *
     * @param dimensionCode 维度编码
     * @param userId        用户ID
     * @return 维度值列表
     */
    List<DimensionValueDto> getAccessibleValues(String dimensionCode, Long userId);
}
