package com.dusk.module.ddm.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.module.auth.enums.ELevel;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * @author kefuming
 * @CreateTime 2022-10-27
 */
@Setter
@Getter
public class CreateExternalUserInput {

    @NotBlank(message = "姓名不能为空")
    @ApiModelProperty("姓名")
    @Length(max = 20, message = "姓名过长")
    private String name;

    @NotNull(message = "组织机构id不能为空")
    @ApiModelProperty("组织单位")
    private Long orgaId;

    // @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式有误")
    @ApiModelProperty("手机号")
    private String phoneNo;

    @ApiModelProperty("身份证号")
    // @NotBlank(message = "身份证号不能为空")
    // @Pattern(regexp = "(^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)|(^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}$)", message = "身份证号格式有误")
    private String idCard;

    @ApiModelProperty("邮箱地址")
    private String emailAddress;

    @ApiModelProperty("门禁卡号")
    private String accessCard;

    @ApiModelProperty("工号")
    private String workNumber;

    @ApiModelProperty("入厂时间")
    private LocalDate enterDate;

    @ApiModelProperty("人脸照片")
    private String facePicture;

    @ApiModelProperty("员工级别")
    private ELevel level;

    @ApiModelProperty("管理的组织")
    private List<Long> managerOrgIds;

    @ApiModelProperty("账号信息")
    private ExternalUserSettingDto dto;
}
