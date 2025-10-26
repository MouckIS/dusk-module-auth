package com.dusk.module.auth.dto.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-12-11 10:47
 */
@Getter
@Setter
public class PermissionInputDto {
    @Schema(description = "权限名称")
    private String name;
    @Schema(description = "是否已授权")
    private boolean granted;
}
