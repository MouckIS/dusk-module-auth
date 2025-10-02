package com.dusk.module.auth.enums;

import lombok.Getter;
import com.dusk.common.core.entity.BaseEnum;

/**
 *
 * @author caiwenjun
 * @date 2024/1/18 10:25
 */
@Getter
public enum UserPrintType implements BaseEnum {
    INNER(1, "本单位"),
    EXTERNAL(2, "外单位"),
    CLEANING(3, "保洁"),
    ;

    private final int value;
    private final String displayName;

    UserPrintType(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
}
