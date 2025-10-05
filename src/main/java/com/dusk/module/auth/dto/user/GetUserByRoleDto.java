package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.enums.EUnitType;

import jakarta.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2021/8/4 16:00
 */

@Getter
@Setter
public class GetUserByRoleDto extends PagedAndSortedInputDto {
    @ApiModelProperty("角色id")
    @NotNull(message = "角色id不能为空")
    private Long roleId;

    @ApiModelProperty("过滤用户名")
    private String userName;

    @ApiModelProperty("用户类型")
    private EUnitType userType;
}
