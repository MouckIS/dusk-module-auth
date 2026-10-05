package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;

public enum OrgLabel implements BaseEnum {
    Other(0, "其他"),
    Department(1, "部门"),
    WorkTeam(2, "班组");


    private final int value;
    private final String displayName;

    OrgLabel(int value, String displayName) {
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
