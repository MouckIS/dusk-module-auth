package com.dusk.module.auth.enums.dashboard;

import com.dusk.common.core.entity.BaseEnum;

/**
 * @author 简建鸿
 * @date 2021-07-26 8:29
 * 仪表盘模块类型
 */
public enum DashboardModuleType implements BaseEnum {
    Center(0, "数据统计项"),
    Business(1, "业务统计项"),
    Description(2, "描述统计项"),
    ThirdParty(3, "第三方统计项");

    private final int value;
    private final String displayName;


    DashboardModuleType(int value, String displayName) {
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
