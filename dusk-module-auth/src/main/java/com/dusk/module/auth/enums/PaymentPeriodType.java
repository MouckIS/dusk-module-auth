package com.dusk.module.auth.enums;

import lombok.Getter;

/**
 * @author kefuming
 * @date 2020-05-08 9:35
 */
@Getter
public enum PaymentPeriodType {
    MONTHLY(30),
    ANNUAL(365);

    private final int days;

    PaymentPeriodType(int days) {
        this.days = days;
    }

}
