package com.dusk.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2022/1/5
 */
@Getter
@Setter
@ApiModel(description = "导入模块统计项权限")
public class ModuleItemPermissionInput {
    @ApiModelProperty("中心模块统计项权限")
    private List<String> centerItemPermission;

    @ApiModelProperty("业务模块统计项权限")
    private List<String> businessItemPermission;

}
