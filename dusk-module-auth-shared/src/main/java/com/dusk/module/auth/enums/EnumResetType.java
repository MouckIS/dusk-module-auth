package com.dusk.module.auth.enums;

import com.dusk.common.core.entity.BaseEnum;
import lombok.Getter;

/**
 * @author kefuming
 * @date 2020-09-22 14:37
 */
@Getter
public enum EnumResetType implements BaseEnum {
    /**
     * 按天重置序列号
     */
    Day(1, "日"),
    /**
     * 按月重置序列号
     */
    Month(2, "月"),
    /**
     * 按年重置序列号
     */
    Year(3, "年"),
    /**
     * 永远不重置
     */
    Never(0, "不重置");

    private final int value;

    private final String displayName;

    EnumResetType(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public static EnumResetType getByName(String resetType) {
        for (EnumResetType type : EnumResetType.values()) {
            if (type.name().equals(resetType))
                return type;
        }
        return null;
    }
}
