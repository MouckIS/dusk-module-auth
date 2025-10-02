package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;

/**
 * @author kefuming
 * @date 2020-10-19 8:29
 */
public enum ToDoMQTTTypeEnum implements BaseEnum {
    ADD(0, "新增代办"),
    FINISH(1, "结束代办"),
    IGNORE(2, "忽略代办");

    private final int value;
    private final String displayName;


    ToDoMQTTTypeEnum(int value, String displayName) {
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
