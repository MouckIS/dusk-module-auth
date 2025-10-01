package com.dusk.module.auth.dto.auditlog;

import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("方法返回值")
    private String result;
}
