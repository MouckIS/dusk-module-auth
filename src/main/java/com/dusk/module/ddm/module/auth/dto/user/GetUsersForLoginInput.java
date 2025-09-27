package com.dusk.module.ddm.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020/5/18 11:08
 */
@Data
public class GetUsersForLoginInput {
    @ApiModelProperty("姓名")
    private String name;
    @ApiModelProperty("姓名拼音")
    private String surName;
    @ApiModelProperty("账户名")
    private String userName;
}
