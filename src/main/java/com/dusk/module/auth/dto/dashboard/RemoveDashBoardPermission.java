package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class RemoveDashBoardPermission {
    /**
     * 主题ID
     */
    @NotNull(message = "主题Id不能为空")
    @Schema(description = "主题Id")
    private Long themeId;

    /**
     * 角色ID列表
     */
    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID")
    private Long roleId;
}
