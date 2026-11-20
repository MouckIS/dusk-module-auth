package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;

/**
 * @author kefuming
 * @date 2020-08-04 14:21
 */
public enum ToDoTargetType implements BaseEnum {
    /**
     * 指定用户id
     */
    UserId,
    /**
     * 指定权限
     */
    Permission,
    /**
     * 指定角色名称（中文名）
     */
    Role;

    @Override
    public int getValue() {
        return ordinal();
    }

    @Override
    public String getDisplayName() {
        return name();
    }
}
