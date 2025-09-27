package com.dusk.module.ddm.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.enums.ELevel;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * @author kefuming
 * @CreateTime 2022-10-25
 */
@Getter
@Setter
public class PersonalInfoInput extends EntityDto {

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
    private String phoneNo;

    @ApiModelProperty("身份证号")
    private String idCard;

    @ApiModelProperty("入厂时间")
    private LocalDate enterDate;

    @ApiModelProperty("门禁卡号")
    private String accessCard;

    @ApiModelProperty("组织单位")
    @NotNull(message = "组织机构id不能为空")
    private Long orgaId;

    @ApiModelProperty("管理的组织机构")
    private List<Long> managerOrgIds;

    @ApiModelProperty("员工级别")
    private ELevel level;
}
