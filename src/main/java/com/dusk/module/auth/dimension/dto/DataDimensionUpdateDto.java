package com.dusk.module.auth.dimension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 更新数据维度请求DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "更新数据维度请求")
public class DataDimensionUpdateDto implements Serializable {

    @NotNull(message = "维度ID不能为空")
    @Schema(description = "维度ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "维度名称不能为空")
    @Size(max = 100, message = "维度名称长度不能超过100")
    @Schema(description = "维度名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dimensionName;

    @Size(max = 500, message = "维度描述长度不能超过500")
    @Schema(description = "维度描述")
    private String dimensionDesc;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @NotNull(message = "版本号不能为空")
    @Schema(description = "版本号（乐观锁）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;
}
