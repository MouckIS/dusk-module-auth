package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.enums.EUnitType;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-12-10 9:19
 */
@Getter
@Setter
public class UserForSelectDto extends EntityDto {
    @ApiModelProperty("姓名")
    private String name;
    @ApiModelProperty("姓名拼音")
    private String surName;
    @ApiModelProperty("账号")
    private String userName;
    @ApiModelProperty("电子邮箱地址")
    private String emailAddress;
    @ApiModelProperty("电话号码")
    private String phoneNo;
    @ApiModelProperty("用户类型")
    private EUnitType userType;
    @ApiModelProperty("角色列表")
    private List<UserListRoleDto> userRoles;
}
