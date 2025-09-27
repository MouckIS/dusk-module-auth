package com.dusk.module.ddm.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2022/1/5
 */
@Data
@ApiModel(description = "导入模块统计项权限")
public class ModuleItemPermissionInput {
    @ApiModelProperty("中心模块统计项权限")
    private List<String> centerItemPermission;

    @ApiModelProperty("业务模块统计项权限")
    private List<String> businessItemPermission;

}
