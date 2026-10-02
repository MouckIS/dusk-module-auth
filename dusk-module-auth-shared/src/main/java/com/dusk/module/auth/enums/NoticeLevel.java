package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;
import lombok.Getter;


public enum NoticeLevel implements BaseEnum {
    NORMAL(0, "普通"),
    HIGH(1, "高"),
    TOP(2, "紧急");

    @Getter
    private final int value;
    @Getter
    private final String displayName;

    NoticeLevel(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
}
