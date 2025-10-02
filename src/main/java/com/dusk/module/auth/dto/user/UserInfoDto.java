package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 个人信息更新 dto
 * @author kefuming
 * @CreateTime 2022-11-08
 */
@Getter
@Setter
public class UserInfoDto {

    @NotBlank(message = "姓名不能为空")
    @ApiModelProperty("用户名")
    @Length(max = 20, message = "姓名过长")
    private String name;

    @ApiModelProperty("工号")
    private String workNumber;

    @ApiModelProperty("岗位")
    private String job;

    @ApiModelProperty("邮箱地址")
    private String emailAddress;

    @ApiModelProperty("手机号")
    @NotBlank(message = "手机号不能为空")
    private String phoneNo;

    @ApiModelProperty("身份证号")
    private String idCard;

    @ApiModelProperty("入厂时间")
    private LocalDate enterDate;

    @ApiModelProperty("门禁卡号")
    private String accessCard;
}
