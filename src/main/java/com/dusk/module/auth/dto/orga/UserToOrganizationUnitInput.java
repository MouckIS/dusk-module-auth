package com.dusk.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-13 15:40
 */
@Getter
@Setter
public class UserToOrganizationUnitInput {
    @ApiModelProperty("用户id")
    private Long userId;

    @ApiModelProperty("组织机构id")
    private Long organizationUnitId;
}
