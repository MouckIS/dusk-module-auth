package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private String phoneNo;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "入厂时间")
    private LocalDate enterDate;

    @Schema(description = "门禁卡号")
    private String accessCard;

    @Schema(description = "组织单位")
    @NotNull(message = "组织机构id不能为空")
    private Long orgaId;

    @Schema(description = "管理的组织机构")
    private List<Long> managerOrgIds;

    @Schema(description = "员工级别")
    private ELevel level;
}
