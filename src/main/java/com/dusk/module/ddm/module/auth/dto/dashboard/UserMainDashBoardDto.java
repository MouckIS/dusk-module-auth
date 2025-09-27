package com.dusk.module.ddm.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author jianjianhong
 * @date 2021-09-09 10:10
 */
@Data
public class UserMainDashBoardDto {
    @ApiModelProperty("首页大屏ID")
    private Long mainDashBoardId;

    @ApiModelProperty("是否有权限")
    private Boolean permission;

    public UserMainDashBoardDto() {
    }

    public UserMainDashBoardDto(Long mainDashBoardId, Boolean permission) {
        this.mainDashBoardId = mainDashBoardId;
        this.permission = permission;
    }
}
