package com.dusk.module.auth.dto.user;

import com.github.dozermapper.core.Mapping;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020/5/18 8:20
 */
@Data
public class UserRoleDto {

    @ApiModelProperty("角色id")
    @Mapping("id")
    private Long roleId;
    @ApiModelProperty("角色名称")
    private String roleName;
    @ApiModelProperty("角色代码")
    private String roleCode;
    @ApiModelProperty("是否分配")
    private boolean isAssigned;
}
