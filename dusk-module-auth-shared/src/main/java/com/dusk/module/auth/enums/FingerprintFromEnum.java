package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;

/**
 * @author kefuming
 * @date 2021-12-03 16:45
 */
public enum FingerprintFromEnum implements BaseEnum {
    UNKNOWN(0, "未知"),
    LIVE20R(1, "指纹采集器Live20R"),
    CABINET(2, "柜子");

    final int value;
    final String displayName;

    FingerprintFromEnum(int value, String displayName) {
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