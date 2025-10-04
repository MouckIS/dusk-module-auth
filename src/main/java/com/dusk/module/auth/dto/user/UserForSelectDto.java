package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "姓名拼音")
    private String surName;
    @Schema(description = "账号")
    private String userName;
    @Schema(description = "电子邮箱地址")
    private String emailAddress;
    @Schema(description = "电话号码")
    private String phoneNo;
    @Schema(description = "用户类型")
    private EUnitType userType;
    @Schema(description = "角色列表")
    private List<UserListRoleDto> userRoles;
}
