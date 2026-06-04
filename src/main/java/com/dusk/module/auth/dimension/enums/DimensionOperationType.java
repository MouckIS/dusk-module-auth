package com.dusk.module.auth.dimension.enums;

import com.dusk.common.core.entity.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据维度操作类型枚举
 *
 * @author dusk
 */
@Getter
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public enum DimensionOperationType implements BaseEnum {

    ADD(0, "新增"),
    UPDATE(1, "修改"),
    DELETE(2, "删除"),
    IMPORT(3, "导入"),
    EXPORT(4, "导出"),
    GRANT_PERMISSION(5, "授权"),
    REVOKE_PERMISSION(6, "撤销授权"),
    BATCH_GRANT_PERMISSION(7, "批量授权"),
    BATCH_REVOKE_PERMISSION(8, "批量撤销授权");

    private final int value;
    private final String displayName;
}
