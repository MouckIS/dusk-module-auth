package com.dusk.module.auth.dimension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 数据维度CSV导入/导出DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "数据维度CSV导入/导出")
public class DataDimensionCsvDto implements Serializable {

    @Schema(description = "维度名称")
    private String dimensionName;

    @Schema(description = "维度编码")
    private String dimensionCode;

    @Schema(description = "维度描述")
    private String dimensionDesc;

    @Schema(description = "是否启用（true/false）")
    private String enabled;
}
