package com.dusk.module.auth.dto.user;

import com.dusk.module.auth.enums.ELevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

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
    @Schema(description = "姓名")
    @Length(max = 20, message = "姓名过长")
    private String name;

    @NotNull(message = "组织机构id不能为空")
    @Schema(description = "组织单位")
    private Long orgaId;

    // @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式有误")
    @Schema(description = "手机号")
    private String phoneNo;

    @Schema(description = "身份证号")
    // @NotBlank(message = "身份证号不能为空")
    // @Pattern(regexp = "(^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)|(^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}$)", message = "身份证号格式有误")
    private String idCard;

    @Schema(description = "邮箱地址")
    private String emailAddress;

    @Schema(description = "门禁卡号")
    private String accessCard;

    @Schema(description = "工号")
    private String workNumber;

    @Schema(description = "入厂时间")
    private LocalDate enterDate;

    @Schema(description = "人脸照片")
    private String facePicture;

    @Schema(description = "员工级别")
    private ELevel level;

    @Schema(description = "管理的组织")
    private List<Long> managerOrgIds;

    @Schema(description = "账号信息")
    private ExternalUserSettingDto dto;
}
