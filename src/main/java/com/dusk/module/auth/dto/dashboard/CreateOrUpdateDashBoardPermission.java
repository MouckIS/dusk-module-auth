package com.dusk.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.EntityDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class CreateOrUpdateDashBoardPermission {
    /**
     * 主题ID
     */
    @NotNull(message = "主题Id不能为空")
    @ApiModelProperty("主题Id")
    private Long themeId;

    /**
     * 角色ID列表
     */
    @ApiModelProperty("角色ID列表")
    private List<Long> roleIds;
}
