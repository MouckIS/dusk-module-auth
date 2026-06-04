package com.dusk.module.auth.dimension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 维度值校验请求DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "维度值校验请求")
public class DimensionValueValidateDto implements Serializable {

    @NotBlank(message = "维度编码不能为空")
    @Schema(description = "维度编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dimensionCode;

    @NotNull(message = "维度值编码列表不能为空")
    @Schema(description = "维度值编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> valueCodes;
}
