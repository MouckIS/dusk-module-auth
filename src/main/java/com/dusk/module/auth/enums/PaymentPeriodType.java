package com.dusk.module.auth.enums;

/**
 * @author kefuming
 * @date 2020-05-08 9:35
 */
public enum PaymentPeriodType {
    Monthly(30),
    Annual(365);

    private final int days;
    PaymentPeriodType(int days){
        this.days = days;
    }

    public int getDays(){
        return days;
    }
}
