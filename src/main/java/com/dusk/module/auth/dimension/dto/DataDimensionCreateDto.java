package com.dusk.module.auth.dimension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 创建数据维度请求DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "创建数据维度请求")
public class DataDimensionCreateDto implements Serializable {

    @NotBlank(message = "维度名称不能为空")
    @Size(max = 100, message = "维度名称长度不能超过100")
    @Schema(description = "维度名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dimensionName;

    @NotBlank(message = "维度编码不能为空")
    @Size(max = 100, message = "维度编码长度不能超过100")
    @Schema(description = "维度编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dimensionCode;

    @Size(max = 500, message = "维度描述长度不能超过500")
    @Schema(description = "维度描述")
    private String dimensionDesc;
}
