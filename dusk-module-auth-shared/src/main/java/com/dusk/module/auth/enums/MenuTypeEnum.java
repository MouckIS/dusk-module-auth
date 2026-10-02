package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;

/**
 * @author jianjianhong
 * @date 2022-08-17 8:29
 */
public enum MenuTypeEnum implements BaseEnum {
    SYSTEM(0, "系统菜单"),
    CUSTOM(1, "自定义菜单");

    private final int value;
    private final String displayName;


    MenuTypeEnum(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }


    @Override
    public int getValue() {
        return value;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
