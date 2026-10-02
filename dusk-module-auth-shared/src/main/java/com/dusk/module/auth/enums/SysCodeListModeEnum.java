package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;

/**
 * 系统常量查询模式枚举
 */
public enum SysCodeListModeEnum implements BaseEnum {
    ALL(0, "返回全部数据"),
    ENABLED(1, "返回有效数据"),
    DISABLED(2, "返回无效数据");

    private final int value;
    private final String displayName;

    SysCodeListModeEnum(int value, String displayName) {
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
