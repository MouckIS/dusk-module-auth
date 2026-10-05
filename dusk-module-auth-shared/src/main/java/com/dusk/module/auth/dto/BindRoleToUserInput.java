package com.dusk.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liangjun
 * @date 2021/8/4 15:35
 */
@Getter
@Setter
public class BindRoleToUserInput implements Serializable {
    /**
     * 设置给用户的角色id
     */
    @Schema(description = "设置给用户的角色id")
    @NotNull(message = "roleId不能为空")
    private Long roleId;

    /**
     * 需要设置角色的userIds
     */
    @Schema(description = "需要设置角色的userIds")
    private List<Long> userIds = new ArrayList<>();
}
