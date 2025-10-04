package com.dusk.module.auth.dto.user;

import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.EntityDto;
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
    @Schema(description = "姓名")
    private String name;

    @Schema(description = "工号")
    private String workNumber;

    @Schema(description = "岗位")
    private String job;

    @Schema(description = "组织单位")
    private List<UserOrgaDto> orgaDtos;

    @Schema(description = "直接上级")
    private String superior;

    @Schema(description = "邮箱地址")
    private String emailAddress;

    @Schema(description = "手机号")
    @Length(min = 11, max = 11, message = "手机号只能为11位")
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式有误")
    private String phoneNo;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "入厂时间")
    private LocalDate enterDate;

    @Schema(description = "门禁卡号")
    private String accessCard;

    @Schema(description = "员工级别")
    private ELevel level;

    @Schema(description = "用户类型")
    private EUnitType userType;

    @Schema(description = "用户状态")
    private UserStatus userStatus;

    @Schema(description = "管理的组织")
    private List<UserOrgaDto> managerDtos;
}
