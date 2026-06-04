package com.dusk.module.auth.dimension.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.dusk.common.core.exception.ResourceNotFoundException;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.dimension.dto.BatchUserDimensionPermissionDto;
import com.dusk.module.auth.dimension.dto.UserDimensionPermissionDto;
import com.dusk.module.auth.dimension.dto.UserDimensionPermissionGrantDto;
import com.dusk.module.auth.dimension.entity.DataDimension;
import com.dusk.module.auth.dimension.entity.DimensionValue;
import com.dusk.module.auth.dimension.entity.UserDimensionPermission;
import com.dusk.module.auth.dimension.enums.DimensionOperationType;
import com.dusk.module.auth.dimension.enums.DimensionTargetType;
import com.dusk.module.auth.dimension.repository.IDataDimensionRepository;
import com.dusk.module.auth.dimension.repository.IDimensionValueRepository;
import com.dusk.module.auth.dimension.repository.IUserDimensionPermissionRepository;
import com.dusk.module.auth.dimension.service.IDimensionOperationLogService;
import com.dusk.module.auth.dimension.service.IUserDimensionPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户维度权限服务实现
 *
 * @author dusk
 */
@Service
public class UserDimensionPermissionServiceImpl extends BaseService<UserDimensionPermission, IUserDimensionPermissionRepository>
        implements IUserDimensionPermissionService {

    @Autowired
    private IDataDimensionRepository dataDimensionRepository;

    @Autowired
    private IDimensionValueRepository dimensionValueRepository;

    @Autowired
    private IDimensionOperationLogService operationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(UserDimensionPermissionGrantDto dto) {
        // 校验维度是否存在
        DataDimension dimension = dataDimensionRepository.findById(dto.getDimensionId())
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，ID: " + dto.getDimensionId()));

        // 获取已有权限
        List<UserDimensionPermission> existing = repository.findByUserIdAndDimensionId(dto.getUserId(), dto.getDimensionId());
        Set<Long> existingValueIds = existing.stream()
                .map(UserDimensionPermission::getDimensionValueId)
                .collect(Collectors.toSet());

        List<String> grantedNames = new ArrayList<>();
        for (Long valueId : dto.getDimensionValueIds()) {
            if (!existingValueIds.contains(valueId)) {
                // 校验维度值是否存在
                DimensionValue dimValue = dimensionValueRepository.findById(valueId)
                        .orElseThrow(() -> new ResourceNotFoundException("维度值不存在，ID: " + valueId));

                UserDimensionPermission permission = new UserDimensionPermission();
                permission.setUserId(dto.getUserId());
                permission.setDimensionId(dto.getDimensionId());
                permission.setDimensionValueId(valueId);
                repository.save(permission);
                grantedNames.add(dimValue.getValueName());
            }
        }

        if (!grantedNames.isEmpty()) {
            operationLogService.log(DimensionOperationType.GRANT_PERMISSION, DimensionTargetType.PERMISSION,
                    dto.getUserId(), "用户ID: " + dto.getUserId(),
                    "授权维度[" + dimension.getDimensionName() + "]的维度值: " + String.join(", ", grantedNames));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long userId, Long dimensionValueId) {
        repository.deleteByUserIdAndDimensionValueId(userId, dimensionValueId);

        operationLogService.log(DimensionOperationType.REVOKE_PERMISSION, DimensionTargetType.PERMISSION,
                userId, "用户ID: " + userId,
                "撤销维度值权限，维度值ID: " + dimensionValueId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeByDimension(Long userId, Long dimensionId) {
        repository.deleteByUserIdAndDimensionId(userId, dimensionId);

        operationLogService.log(DimensionOperationType.REVOKE_PERMISSION, DimensionTargetType.PERMISSION,
                userId, "用户ID: " + userId,
                "撤销维度下所有权限，维度ID: " + dimensionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchGrant(BatchUserDimensionPermissionDto dto) {
        DataDimension dimension = dataDimensionRepository.findById(dto.getDimensionId())
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，ID: " + dto.getDimensionId()));

        for (Long userId : dto.getUserIds()) {
            for (Long valueId : dto.getDimensionValueIds()) {
                if (!repository.existsByUserIdAndDimensionValueId(userId, valueId)) {
                    // 校验维度值是否存在
                    dimensionValueRepository.findById(valueId)
                            .orElseThrow(() -> new ResourceNotFoundException("维度值不存在，ID: " + valueId));

                    UserDimensionPermission permission = new UserDimensionPermission();
                    permission.setUserId(userId);
                    permission.setDimensionId(dto.getDimensionId());
                    permission.setDimensionValueId(valueId);
                    repository.save(permission);
                }
            }
        }

        operationLogService.log(DimensionOperationType.BATCH_GRANT_PERMISSION, DimensionTargetType.PERMISSION,
                null, "批量操作",
                "批量授权 " + dto.getUserIds().size() + " 个用户访问维度[" + dimension.getDimensionName() + "]的 "
                        + dto.getDimensionValueIds().size() + " 个维度值");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRevoke(BatchUserDimensionPermissionDto dto) {
        DataDimension dimension = dataDimensionRepository.findById(dto.getDimensionId())
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，ID: " + dto.getDimensionId()));

        for (Long valueId : dto.getDimensionValueIds()) {
            repository.deleteByUserIdInAndDimensionValueId(dto.getUserIds(), valueId);
        }

        operationLogService.log(DimensionOperationType.BATCH_REVOKE_PERMISSION, DimensionTargetType.PERMISSION,
                null, "批量操作",
                "批量撤销 " + dto.getUserIds().size() + " 个用户在维度[" + dimension.getDimensionName() + "]下的 "
                        + dto.getDimensionValueIds().size() + " 个维度值权限");
    }

    @Override
    public List<UserDimensionPermissionDto> getByUserId(Long userId) {
        List<UserDimensionPermission> permissions = repository.findByUserId(userId);
        return permissions.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<UserDimensionPermissionDto> getByUserIdAndDimensionId(Long userId, Long dimensionId) {
        List<UserDimensionPermission> permissions = repository.findByUserIdAndDimensionId(userId, dimensionId);
        return permissions.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(Long userId, Long dimensionValueId) {
        return repository.existsByUserIdAndDimensionValueId(userId, dimensionValueId);
    }

    @Override
    public List<Long> getAccessibleValueIds(Long userId, String dimensionCode) {
        DataDimension dimension = dataDimensionRepository.findByDimensionCode(dimensionCode)
                .orElse(null);
        if (dimension == null) {
            return Collections.emptyList();
        }

        List<UserDimensionPermission> permissions = repository.findByUserIdAndDimensionId(userId, dimension.getId());
        return permissions.stream()
                .map(UserDimensionPermission::getDimensionValueId)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAccessibleValueCodes(Long userId, String dimensionCode) {
        DataDimension dimension = dataDimensionRepository.findByDimensionCode(dimensionCode)
                .orElse(null);
        if (dimension == null) {
            return Collections.emptyList();
        }

        List<UserDimensionPermission> permissions = repository.findByUserIdAndDimensionId(userId, dimension.getId());
        Set<Long> valueIds = permissions.stream()
                .map(UserDimensionPermission::getDimensionValueId)
                .collect(Collectors.toSet());

        if (valueIds.isEmpty()) {
            return Collections.emptyList();
        }

        return dimensionValueRepository.findByDimensionIdOrderBySortIndexAsc(dimension.getId()).stream()
                .filter(v -> valueIds.contains(v.getId()))
                .map(DimensionValue::getValueCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAccessibleValueNames(Long userId, String dimensionCode) {
        DataDimension dimension = dataDimensionRepository.findByDimensionCode(dimensionCode)
                .orElse(null);
        if (dimension == null) {
            return Collections.emptyList();
        }

        List<UserDimensionPermission> permissions = repository.findByUserIdAndDimensionId(userId, dimension.getId());
        Set<Long> valueIds = permissions.stream()
                .map(UserDimensionPermission::getDimensionValueId)
                .collect(Collectors.toSet());

        if (valueIds.isEmpty()) {
            return Collections.emptyList();
        }

        return dimensionValueRepository.findByDimensionIdOrderBySortIndexAsc(dimension.getId()).stream()
                .filter(v -> valueIds.contains(v.getId()))
                .map(DimensionValue::getValueName)
                .collect(Collectors.toList());
    }

    private UserDimensionPermissionDto toDto(UserDimensionPermission entity) {
        UserDimensionPermissionDto dto = new UserDimensionPermissionDto();
        BeanUtil.copyProperties(entity, dto);

        // 填充维度名称
        dataDimensionRepository.findById(entity.getDimensionId()).ifPresent(dim -> {
            dto.setDimensionName(dim.getDimensionName());
        });

        // 填充维度值名称
        dimensionValueRepository.findById(entity.getDimensionValueId()).ifPresent(val -> {
            dto.setValueName(val.getValueName());
            dto.setValueCode(val.getValueCode());
        });

        return dto;
    }
}
