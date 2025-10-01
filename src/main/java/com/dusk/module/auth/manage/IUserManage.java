package com.dusk.module.auth.manage;

import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-08-05 15:49
 */
public interface IUserManage {
    /**
     * 获取指定id的角色列表
     *
     * @param userId
     * @return
     */
    List<Role> getUserRole(Long userId);

    /**
     * 获取当前登陆人的信息
     *
     * @return
     */
    User getCurrentUser();

    /**
     * 获取指定登陆人的信息
     *
     * @param userId
     * @return
     */
    User getUserInfo(Long userId);

    /**
     * 获取当前登陆人的权限
     *
     * @return
     */
    List<String> getCurrentUserPermissions();

    /**
     * 获取指定登陆人的权限
     *
     * @param userId
     * @return
     */
    List<String> getUserPermissions(Long userId);


    /**
     * 根据角色名获取用户id清单
     *
     * @param roleNames
     * @return
     */
    List<Long> getUserIdsByRoleName(List<String> roleNames);
}
