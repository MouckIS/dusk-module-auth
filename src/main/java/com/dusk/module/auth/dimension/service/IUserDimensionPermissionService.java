package com.dusk.module.auth.dimension.service;

import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.dimension.dto.BatchUserDimensionPermissionDto;
import com.dusk.module.auth.dimension.dto.UserDimensionPermissionDto;
import com.dusk.module.auth.dimension.dto.UserDimensionPermissionGrantDto;
import com.dusk.module.auth.dimension.entity.UserDimensionPermission;
import com.dusk.module.auth.dimension.repository.IUserDimensionPermissionRepository;

import java.util.List;

/**
 * 用户维度权限服务接口
 *
 * @author dusk
 */
public interface IUserDimensionPermissionService extends IBaseService<UserDimensionPermission, IUserDimensionPermissionRepository> {

    /**
     * 给用户授权维度值
     *
     * @param dto 授权请求
     */
    void grant(UserDimensionPermissionGrantDto dto);

    /**
     * 撤销用户的维度值权限
     *
     * @param userId           用户ID
     * @param dimensionValueId 维度值ID
     */
    void revoke(Long userId, Long dimensionValueId);

    /**
     * 撤销用户在某个维度下的所有权限
     *
     * @param userId      用户ID
     * @param dimensionId 维度ID
     */
    void revokeByDimension(Long userId, Long dimensionId);

    /**
     * 批量授权
     *
     * @param dto 批量授权请求
     */
    void batchGrant(BatchUserDimensionPermissionDto dto);

    /**
     * 批量撤销授权
     *
     * @param dto 批量撤销请求
     */
    void batchRevoke(BatchUserDimensionPermissionDto dto);

    /**
     * 查询用户的维度权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<UserDimensionPermissionDto> getByUserId(Long userId);

    /**
     * 查询用户在某个维度下的权限列表
     *
     * @param userId      用户ID
     * @param dimensionId 维度ID
     * @return 权限列表
     */
    List<UserDimensionPermissionDto> getByUserIdAndDimensionId(Long userId, Long dimensionId);

    /**
     * 判断用户是否有某个维度值的权限
     *
     * @param userId           用户ID
     * @param dimensionValueId 维度值ID
     * @return 是否有权限
     */
    boolean hasPermission(Long userId, Long dimensionValueId);

    /**
     * 获取用户有权限的维度值ID列表
     *
     * @param userId        用户ID
     * @param dimensionCode 维度编码
     * @return 维度值ID列表
     */
    List<Long> getAccessibleValueIds(Long userId, String dimensionCode);

    /**
     * 获取用户有权限的维度值编码列表
     *
     * @param userId        用户ID
     * @param dimensionCode 维度编码
     * @return 维度值编码列表
     */
    List<String> getAccessibleValueCodes(Long userId, String dimensionCode);

    /**
     * 获取用户有权限的维度值名称列表
     *
     * @param userId        用户ID
     * @param dimensionCode 维度编码
     * @return 维度值名称列表
     */
    List<String> getAccessibleValueNames(Long userId, String dimensionCode);
}
