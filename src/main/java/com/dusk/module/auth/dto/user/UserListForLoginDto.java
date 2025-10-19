package com.dusk.module.auth.dto.user;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/5/18 11:12
 */
@Getter
@Setter
public class UserListForLoginDto extends EntityDto {

    @ApiModelProperty("姓名")
    private String name;
    @ApiModelProperty("姓名拼音")
    private String surName;
    @ApiModelProperty("账户名")
    private String userName;
}
