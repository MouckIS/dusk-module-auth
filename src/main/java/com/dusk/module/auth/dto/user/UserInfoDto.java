package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "用户名")
    @Length(max = 20, message = "姓名过长")
    private String name;

    @Schema(description = "工号")
    private String workNumber;

    @Schema(description = "岗位")
    private String job;

    @Schema(description = "邮箱地址")
    private String emailAddress;

    @Schema(description = "手机号")
    @NotBlank(message = "手机号不能为空")
    private String phoneNo;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "入厂时间")
    private LocalDate enterDate;

    @Schema(description = "门禁卡号")
    private String accessCard;
}
