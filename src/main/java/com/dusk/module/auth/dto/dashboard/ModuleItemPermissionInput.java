package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2022/1/5
 */
@Data
@Schema(description = "导入模块统计项权限")
public class ModuleItemPermissionInput {
    @Schema(description = "中心模块统计项权限")
    private List<String> centerItemPermission;

    @Schema(description = "业务模块统计项权限")
    private List<String> businessItemPermission;

}
