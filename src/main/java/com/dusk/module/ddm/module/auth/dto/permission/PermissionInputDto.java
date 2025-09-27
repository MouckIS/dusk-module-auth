package com.dusk.module.ddm.module.auth.dto.permission;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-12-11 10:47
 */
@Getter
@Setter
public class PermissionInputDto {
    @ApiModelProperty("权限名称")
    private String name;
    @ApiModelProperty("是否已授权")
    private boolean granted;
}
