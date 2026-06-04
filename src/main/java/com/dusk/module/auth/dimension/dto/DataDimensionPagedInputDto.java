package com.dusk.module.auth.dimension.dto;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据维度分页查询DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "数据维度分页查询")
public class DataDimensionPagedInputDto extends PagedAndSortedInputDto {

    @Schema(description = "维度名称（模糊搜索）")
    private String dimensionName;

    @Schema(description = "维度编码（模糊搜索）")
    private String dimensionCode;

    @Schema(description = "维度描述（模糊搜索）")
    private String dimensionDesc;

    @Schema(description = "关键字搜索（搜索名称和描述）")
    private String keyword;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
