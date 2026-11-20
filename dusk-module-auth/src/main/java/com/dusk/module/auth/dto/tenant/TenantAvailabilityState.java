package com.dusk.module.auth.dto.tenant;

import com.dusk.common.core.entity.BaseEnum;

/**
 * @author kefuming
 * @date 2020-05-18 14:30
 */
public enum TenantAvailabilityState implements BaseEnum {
    Available,
    InActive,
    NotFound;

    @Override
    public int getValue() {
        return ordinal();
    }

    @Override
    public String getDisplayName() {
        return name();
    }
}
