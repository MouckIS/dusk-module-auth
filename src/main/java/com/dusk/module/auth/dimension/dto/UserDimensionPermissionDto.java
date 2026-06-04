package com.dusk.module.auth.dimension.dto;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户维度权限响应DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "用户维度权限响应")
public class UserDimensionPermissionDto extends EntityDto {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "维度ID")
    private Long dimensionId;

    @Schema(description = "维度值ID")
    private Long dimensionValueId;

    @Schema(description = "维度名称")
    private String dimensionName;

    @Schema(description = "维度值名称")
    private String valueName;

    @Schema(description = "维度值编码")
    private String valueCode;
}
