package com.dusk.module.auth.dimension.dto;

import com.dusk.common.core.dto.AuditedEntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据维度响应DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "数据维度响应")
public class DataDimensionDto extends AuditedEntityDto {

    @Schema(description = "维度名称")
    private String dimensionName;

    @Schema(description = "维度编码")
    private String dimensionCode;

    @Schema(description = "维度描述")
    private String dimensionDesc;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "版本号")
    private Integer version;
}
