package com.dusk.module.auth.dimension.dto;

import com.dusk.common.core.dto.AuditedEntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 维度值响应DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "维度值响应")
public class DimensionValueDto extends AuditedEntityDto {

    @Schema(description = "所属维度ID")
    private Long dimensionId;

    @Schema(description = "维度值编码")
    private String valueCode;

    @Schema(description = "维度值名称")
    private String valueName;

    @Schema(description = "维度值描述")
    private String valueDesc;

    @Schema(description = "排序号")
    private Integer sortIndex;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "版本号")
    private Integer version;
}
