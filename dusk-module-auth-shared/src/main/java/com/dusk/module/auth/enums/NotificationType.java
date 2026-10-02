package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;

/**
 * 消息通知的类型
 *
 * @author 喻黎洋
 * @date 2021/7/30 16:40
 */
public enum NotificationType implements BaseEnum {

    NOTIFICATION(0, "通知"),
    ALERT(1, "告警");

    private final int value;
    private final String displayName;


    NotificationType(int value, String displayName) {
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
