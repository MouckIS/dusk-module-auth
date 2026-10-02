package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;
import lombok.Getter;


public enum NoticeTargetType implements BaseEnum {
    USER(0, "用户"),
    ROLE(1, "角色"),
    ORGANIZATION_UNIT(2, "组织机构"),
    ;

    @Getter
    private final int value;
    @Getter
    private final String displayName;

    NoticeTargetType(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
}
