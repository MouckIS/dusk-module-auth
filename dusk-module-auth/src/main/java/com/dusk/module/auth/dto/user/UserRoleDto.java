package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/5/18 8:20
 */
@Getter
@Setter
public class UserRoleDto {

    @Schema(description = "角色id")
    private Long roleId;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "角色代码")
    private String roleCode;
    @Schema(description = "是否分配")
    private boolean isAssigned;
}
