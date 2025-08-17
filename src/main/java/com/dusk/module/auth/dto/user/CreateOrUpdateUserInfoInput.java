package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.dusk.common.module.auth.dto.CreateOrUpdateUserInput;
import com.dusk.module.auth.enums.ELevel;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * @Author kefuming
 * @CreateTime 2022-11-08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrUpdateUserInfoInput extends CreateOrUpdateUserInput {
    @NotNull
    @ApiModelProperty("员工级别")
    private ELevel level;

    @ApiModelProperty("所属厂站列表")
    private List<Long> stationIds;

    @ApiModelProperty("岗位")
    private String job;

    @ApiModelProperty("卡号")
    private String accessCard;

    @ApiModelProperty("入厂时间")
    private LocalDate enterDate;

    @ApiModelProperty("管理的组织id")
    private List<Long> managerOrgIds;
}
