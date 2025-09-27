package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;

/**
 * 用户级别
 * @author kefuming
 * @CreateTime 2022-11-08
 */
public enum ELevel implements BaseEnum {
    General(0, "普通员工"),
    Manager(1, "管理层");

    private final int value;
    private final String displayName;

    ELevel(int value, String displayName) {
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
