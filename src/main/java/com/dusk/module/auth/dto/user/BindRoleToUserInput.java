package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2021/8/4 15:35
 */
@Data
public class BindRoleToUserInput {
    /**
     * 设置给用户的角色id
     */
    @ApiModelProperty("设置给用户的角色id")
    @NotNull(message = "roleId不能为空")
    private Long roleId;

    /**
     * 需要设置角色的userIds
     */
    @ApiModelProperty("需要设置角色的userIds")
    private List<Long> userIds = new ArrayList<>();
}
