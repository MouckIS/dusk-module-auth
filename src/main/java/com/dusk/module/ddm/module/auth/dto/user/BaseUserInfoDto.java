package com.dusk.module.ddm.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.module.auth.enums.EUnitType;
import com.dusk.common.module.auth.enums.UserStatus;
import com.dusk.module.auth.enums.ELevel;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * @author kefuming
 * @CreateTime 2022-10-25
 */
@Getter
@Setter
public class BaseUserInfoDto extends EntityDto {
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度需要小于64")
    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("工号")
    private String workNumber;

    @ApiModelProperty("岗位")
    private String job;

    @ApiModelProperty("组织单位")
    private List<UserOrgaDto> orgaDtos;

    @ApiModelProperty("直接上级")
    private String superior;

    @ApiModelProperty("邮箱地址")
    private String emailAddress;

    @ApiModelProperty("手机号")
    @Length(min = 11, max = 11, message = "手机号只能为11位")
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式有误")
    private String phoneNo;

    @ApiModelProperty("身份证号")
    private String idCard;

    @ApiModelProperty("入厂时间")
    private LocalDate enterDate;

    @ApiModelProperty("门禁卡号")
    private String accessCard;

    @ApiModelProperty("员工级别")
    private ELevel level;

    @ApiModelProperty("用户类型")
    private EUnitType userType;

    @ApiModelProperty("用户状态")
    private UserStatus userStatus;

    @ApiModelProperty("管理的组织")
    private List<UserOrgaDto> managerDtos;
}
