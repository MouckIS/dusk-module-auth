package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;

import lombok.Getter;

/**
 * @author kefuming
 * @date 2020-05-08 9:35
 */
@Getter
public enum PaymentPeriodType implements BaseEnum {
    MONTHLY(30),
    ANNUAL(365);

    private final int days;

    PaymentPeriodType(int days) {
        this.days = days;
    }


    @Override
    public int getValue() {
        return days;
    }

    @Override
    public String getDisplayName() {
        return name();
    }
}
