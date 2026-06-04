package com.dusk.module.auth.dimension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 批量用户维度权限操作请求DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "批量用户维度权限操作请求")
public class BatchUserDimensionPermissionDto implements Serializable {

    @NotEmpty(message = "用户ID列表不能为空")
    @Schema(description = "用户ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> userIds;

    @NotNull(message = "维度ID不能为空")
    @Schema(description = "维度ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dimensionId;

    @NotEmpty(message = "维度值ID列表不能为空")
    @Schema(description = "维度值ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> dimensionValueIds;
}
