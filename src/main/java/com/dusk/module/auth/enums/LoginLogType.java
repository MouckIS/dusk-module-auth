package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;
import lombok.Getter;

/**
 * @author kefuming
 * @description: 登录日志类型
 * @date 2022/10/28
 */
//@Getter
public enum LoginLogType implements BaseEnum {
    LOGIN_IN(1, "登入"),
    LOGIN_OUT(2, "登出");

    private final int value;
    private final String displayName;

    LoginLogType(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }


    @Override
    public int getValue() {
        return 0;
    }

    @Override
    public String getDisplayName() {
        return "";
    }
}
