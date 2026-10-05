package com.dusk.module.auth.dto;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @Author: pengmengjiang
 * @Date: 2021/2/23 19:30
 */
@Getter
@Setter
public class UserSimpleDto extends EntityDto {
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "姓名拼音")
    private String surName;
    @Schema(description = "账号")
    private String userName;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "电子邮箱地址")
    private String emailAddress;
    @Schema(description = "电话号码")
    private String phoneNo;
    @Schema(description = "是否管理员")
    private boolean admin;
    @Schema(description = "头像图片")
    private Long profilePictureId;
    @Schema(description = "是否确认邮箱")
    private boolean emailConfirmed;
    @Schema(description = "是否激活")
    private boolean active;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "工号")
    public String workNumber;
    @Schema(description = "签字图片")
    public Long signaturePictureId;
}
