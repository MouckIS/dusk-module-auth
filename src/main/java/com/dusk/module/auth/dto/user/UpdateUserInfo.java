package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;

/**
 * @author kefuming
 * @date 2021-09-17 13:51
 */
@Getter
@Setter
public class UpdateUserInfo {
    @ApiModelProperty("姓名")
    @NotBlank(message = "姓名不能为空")
    @Length(max = 20, message = "姓名过长")
    private String name;
    @ApiModelProperty("手机号 ps：尚未支持更换手机号验证")
    private String phoneNo;
    @ApiModelProperty("邮箱地址 ps：尚未支持更换邮箱验证")
    @Length(max = 100, message = "邮箱号过长")
    private String emailAddress;
}
