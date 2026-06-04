package com.dusk.module.auth.dimension.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.exception.ResourceNotFoundException;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.dimension.dto.*;
import com.dusk.module.auth.dimension.entity.DataDimension;
import com.dusk.module.auth.dimension.entity.DimensionValue;
import com.dusk.module.auth.dimension.entity.UserDimensionPermission;
import com.dusk.module.auth.dimension.enums.DimensionOperationType;
import com.dusk.module.auth.dimension.enums.DimensionTargetType;
import com.dusk.module.auth.dimension.repository.IDataDimensionRepository;
import com.dusk.module.auth.dimension.repository.IDimensionValueRepository;
import com.dusk.module.auth.dimension.repository.IUserDimensionPermissionRepository;
import com.dusk.module.auth.dimension.service.IDimensionOperationLogService;
import com.dusk.module.auth.dimension.service.IDimensionValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 维度值服务实现
 *
 * @author dusk
 */
@Service
public class DimensionValueServiceImpl extends BaseService<DimensionValue, IDimensionValueRepository>
        implements IDimensionValueService {

    @Autowired
    private IDataDimensionRepository dataDimensionRepository;

    @Autowired
    private IUserDimensionPermissionRepository userDimensionPermissionRepository;

    @Autowired
    private IDimensionOperationLogService operationLogService;

    @Override
    public List<DimensionValueDto> getByDimensionId(Long dimensionId) {
        List<DimensionValue> list = repository.findByDimensionIdOrderBySortIndexAsc(dimensionId);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<DimensionValueDto> getByDimensionCode(String dimensionCode) {
        DataDimension dimension = dataDimensionRepository.findByDimensionCode(dimensionCode)
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，编码: " + dimensionCode));
        return getByDimensionId(dimension.getId());
    }

    @Override
    public DimensionValueDto getById(Long id) {
        DimensionValue entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("维度值不存在，ID: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DimensionValueDto create(DimensionValueCreateDto dto) {
        // 校验维度是否存在
        DataDimension dimension = dataDimensionRepository.findById(dto.getDimensionId())
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，ID: " + dto.getDimensionId()));

        // 检查值编码唯一性
        if (repository.existsByDimensionIdAndValueCode(dto.getDimensionId(), dto.getValueCode())) {
            throw new BusinessException("该维度下的值编码已存在: " + dto.getValueCode());
        }

        DimensionValue entity = new DimensionValue();
        BeanUtil.copyProperties(dto, entity);
        entity.setEnabled(true);
        entity = repository.save(entity);

        operationLogService.log(DimensionOperationType.ADD, DimensionTargetType.DIMENSION_VALUE,
                entity.getId(), entity.getValueName(),
                "创建维度值: " + entity.getValueName() + " (" + entity.getValueCode() + ")，所属维度: " + dimension.getDimensionName());

        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DimensionValueDto update(DimensionValueUpdateDto dto) {
        DimensionValue entity = repository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("维度值不存在，ID: " + dto.getId()));

        entity.setValueName(dto.getValueName());
        entity.setValueDesc(dto.getValueDesc());
        if (dto.getSortIndex() != null) {
            entity.setSortIndex(dto.getSortIndex());
        }
        if (dto.getEnabled() != null) {
            entity.setEnabled(dto.getEnabled());
        }
        entity.setVersion(dto.getVersion());
        entity = repository.save(entity);

        operationLogService.log(DimensionOperationType.UPDATE, DimensionTargetType.DIMENSION_VALUE,
                entity.getId(), entity.getValueName(),
                "更新维度值: " + entity.getValueName());

        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        DimensionValue entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("维度值不存在，ID: " + id));

        // 删除关联的权限
        userDimensionPermissionRepository.deleteByDimensionValueId(id);
        // 删除维度值
        repository.deleteById(id);

        operationLogService.log(DimensionOperationType.DELETE, DimensionTargetType.DIMENSION_VALUE,
                entity.getId(), entity.getValueName(),
                "删除维度值: " + entity.getValueName() + " (" + entity.getValueCode() + ")");
    }

    @Override
    public DimensionValueValidateResultDto validate(DimensionValueValidateDto dto) {
        DataDimension dimension = dataDimensionRepository.findByDimensionCode(dto.getDimensionCode())
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，编码: " + dto.getDimensionCode()));

        List<DimensionValue> allValues = repository.findByDimensionIdOrderBySortIndexAsc(dimension.getId());
        Set<String> existingCodes = allValues.stream()
                .filter(v -> v.getEnabled() != null && v.getEnabled())
                .map(DimensionValue::getValueCode)
                .collect(Collectors.toSet());

        List<String> validCodes = new ArrayList<>();
        List<String> invalidCodes = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        for (String code : dto.getValueCodes()) {
            if (existingCodes.contains(code)) {
                validCodes.add(code);
            } else {
                invalidCodes.add(code);
                messages.add("维度值编码无效或已禁用: " + code);
            }
        }

        DimensionValueValidateResultDto result = new DimensionValueValidateResultDto();
        result.setValid(invalidCodes.isEmpty());
        result.setValidCodes(validCodes);
        result.setInvalidCodes(invalidCodes);
        result.setMessages(messages);
        return result;
    }

    @Override
    public List<DimensionValueDto> getAccessibleValues(String dimensionCode, Long userId) {
        DataDimension dimension = dataDimensionRepository.findByDimensionCode(dimensionCode)
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，编码: " + dimensionCode));

        // 获取用户有权限的维度值ID
        List<UserDimensionPermission> permissions = userDimensionPermissionRepository
                .findByUserIdAndDimensionId(userId, dimension.getId());
        Set<Long> permittedValueIds = permissions.stream()
                .map(UserDimensionPermission::getDimensionValueId)
                .collect(Collectors.toSet());

        // 如果没有任何权限记录，返回空列表
        if (permittedValueIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取有权限且启用的维度值
        List<DimensionValue> allValues = repository.findByDimensionIdOrderBySortIndexAsc(dimension.getId());
        return allValues.stream()
                .filter(v -> v.getEnabled() != null && v.getEnabled() && permittedValueIds.contains(v.getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private DimensionValueDto toDto(DimensionValue entity) {
        DimensionValueDto dto = new DimensionValueDto();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }
}
