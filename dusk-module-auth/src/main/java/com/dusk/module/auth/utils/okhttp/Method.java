package com.dusk.module.auth.utils.okhttp;

import com.dusk.common.core.entity.BaseEnum;

/**
 * @author jianjianhong
 * @date 2022/8/1
 */
public enum Method implements BaseEnum {
    GET, POST;

    @Override
    public int getValue() {
        return ordinal();
    }

    @Override
    public String getDisplayName() {
        return name();
    }
}