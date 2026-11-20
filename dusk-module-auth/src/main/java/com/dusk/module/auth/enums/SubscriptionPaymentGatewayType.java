package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;

/**
 * @author kefuming
 * @date 2020-05-08 10:29
 */
public enum SubscriptionPaymentGatewayType implements BaseEnum {
    Paypal;

    @Override
    public int getValue() {
        return ordinal();
    }

    @Override
    public String getDisplayName() {
        return name();
    }
}
