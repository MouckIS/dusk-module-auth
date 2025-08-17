package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.EntityDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/15 11:58
 */
@Data
public class UserListDto extends EntityDto {
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
    @ApiModelProperty("是否管理员")
    private boolean admin;
    @ApiModelProperty("头像图片")
    private String profilePictureId;
    @ApiModelProperty("是否确认邮箱")
    private boolean emailConfirmed;
    @ApiModelProperty("角色列表")
    private List<UserListRoleDto> userRoles;
    @ApiModelProperty("是否激活")
    private boolean active;
    @ApiModelProperty("是否被锁定")
    private boolean lock;
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
    @ApiModelProperty("工号")
    public String workNumber;
    @ApiModelProperty("签字图片")
    public Long signaturePictureId;
}
