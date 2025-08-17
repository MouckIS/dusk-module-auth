package com.dusk.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-13 16:42
 */
@Data
public class UserSummaryDto {
    @ApiModelProperty("用户id")
    private String userId;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("账号")
    private String userName;

    @ApiModelProperty("组织机构id")
    private String organizationUnitId;
}
