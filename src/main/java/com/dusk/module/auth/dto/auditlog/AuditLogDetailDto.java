package com.dusk.module.auth.dto.auditlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

/**
 * @author pengmengjiang
 * @date 2020/8/31 09:10
 */
@Data
@FieldNameConstants
public class AuditLogDetailDto extends AuditLogListDto {
    private String exception;
    private String parameters;
    private String orgId;
    @Schema(description = "方法返回值")
    private String result;
}
