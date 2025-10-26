package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2021-09-09 10:10
 */
@Getter
@Setter
public class UserMainDashBoardDto {
    @Schema(description = "首页大屏ID")
    private Long mainDashBoardId;

    @Schema(description = "是否有权限")
    private Boolean permission;

    public UserMainDashBoardDto() {
    }

    public UserMainDashBoardDto(Long mainDashBoardId, Boolean permission) {
        this.mainDashBoardId = mainDashBoardId;
        this.permission = permission;
    }
}
