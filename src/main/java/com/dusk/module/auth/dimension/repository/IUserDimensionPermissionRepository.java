package com.dusk.module.auth.dimension.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.dimension.entity.UserDimensionPermission;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户维度值权限仓储
 *
 * @author dusk
 */
@Repository
public interface IUserDimensionPermissionRepository extends IBaseRepository<UserDimensionPermission> {

    /**
     * 根据用户ID和维度ID查找权限列表
     *
     * @param userId      用户ID
     * @param dimensionId 维度ID
     * @return 权限列表
     */
    List<UserDimensionPermission> findByUserIdAndDimensionId(Long userId, Long dimensionId);

    /**
     * 根据用户ID查找所有权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<UserDimensionPermission> findByUserId(Long userId);

    /**
     * 根据维度值ID查找所有权限
     *
     * @param dimensionValueId 维度值ID
     * @return 权限列表
     */
    List<UserDimensionPermission> findByDimensionValueId(Long dimensionValueId);

    /**
     * 根据用户ID和维度值ID删除权限
     *
     * @param userId           用户ID
     * @param dimensionValueId 维度值ID
     */
    void deleteByUserIdAndDimensionValueId(Long userId, Long dimensionValueId);

    /**
     * 根据用户ID和维度ID删除所有权限
     *
     * @param userId      用户ID
     * @param dimensionId 维度ID
     */
    void deleteByUserIdAndDimensionId(Long userId, Long dimensionId);

    /**
     * 根据维度值ID删除所有权限
     *
     * @param dimensionValueId 维度值ID
     */
    void deleteByDimensionValueId(Long dimensionValueId);

    /**
     * 根据维度ID删除所有权限
     *
     * @param dimensionId 维度ID
     */
    void deleteByDimensionId(Long dimensionId);

    /**
     * 判断用户是否有某个维度值的权限
     *
     * @param userId           用户ID
     * @param dimensionValueId 维度值ID
     * @return 是否存在
     */
    boolean existsByUserIdAndDimensionValueId(Long userId, Long dimensionValueId);

    /**
     * 根据用户ID列表和维度值ID删除权限
     *
     * @param userIds          用户ID列表
     * @param dimensionValueId 维度值ID
     */
    void deleteByUserIdInAndDimensionValueId(List<Long> userIds, Long dimensionValueId);

    /**
     * 根据维度编码查询用户有权限的维度值ID列表
     *
     * @param userId      用户ID
     * @param dimensionId 维度ID
     * @return 维度值ID列表
     */
    List<UserDimensionPermission> findByUserIdAndDimensionIdIn(Long userId, List<Long> dimensionIds);
}
