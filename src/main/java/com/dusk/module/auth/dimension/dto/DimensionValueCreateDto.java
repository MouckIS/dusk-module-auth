package com.dusk.module.auth.dimension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 创建维度值请求DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "创建维度值请求")
public class DimensionValueCreateDto implements Serializable {

    @NotNull(message = "维度ID不能为空")
    @Schema(description = "所属维度ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dimensionId;

    @NotBlank(message = "维度值编码不能为空")
    @Size(max = 100, message = "维度值编码长度不能超过100")
    @Schema(description = "维度值编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String valueCode;

    @NotBlank(message = "维度值名称不能为空")
    @Size(max = 200, message = "维度值名称长度不能超过200")
    @Schema(description = "维度值名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String valueName;

    @Size(max = 500, message = "维度值描述长度不能超过500")
    @Schema(description = "维度值描述")
    private String valueDesc;

    @Schema(description = "排序号")
    private Integer sortIndex;
}
