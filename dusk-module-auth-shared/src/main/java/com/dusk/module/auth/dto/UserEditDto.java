package com.dusk.module.auth.dto;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.enums.EUnitType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * @author duanxiaokang
 * @date 2020/5/15 17:23
 */
@Getter
@Setter
public class UserEditDto extends EntityDto {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度需要小于64")
    @Schema(description = "姓名")
    public String name;
    @Schema(description = "姓名拼音")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String surName;
    @Schema(description = "账号")
    @NotBlank(message = "账号不能为空")
    @Size(max = 64, message = "账号长度需要小于64")
    public String userName;
    @Schema(description = "电子邮件地址")
    public String emailAddress;
    @Schema(description = "电话号码")
    public String phoneNo;
    @Schema(description = "密码")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String password;
    @Schema(description = "是否管理员")
    private boolean admin;
    @Schema(description = "是否激活，激活就显示可选激活开始和结束日期")
    public boolean active;
    @Schema(description = "下次登陆是否强制修改密码")
    public boolean shouldChangePasswordOnNextLogin;
    @Schema(description = "工号")
    public String workNumber;
    @Schema(description = "签字图片")
    public Long signaturePictureId;
    @Schema(description = "头像图片")
    public Long profilePictureId;
    @Schema(description = "身份证号码")
    private String idCard;
    @Schema(description = "用户类型")
    public EUnitType userType = EUnitType.Inner;


    /**
     * 激活开始日期
     */
    @Schema(description = "激活开始日期，可空")
    private LocalDate activeStartDate;

    /**
     * 激活结束日期
     */
    @Schema(description = "激活结束日期，可空")
    private LocalDate activeEndDate;

    @Schema(description = "第三方用户Id，可空")
    private String foreignId;
    @Schema(description = "岗位")
    private String job;
}
