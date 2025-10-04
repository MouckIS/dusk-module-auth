package com.dusk.module.auth.dto.user;

import com.dusk.common.rpc.auth.dto.CreateOrUpdateUserInput;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.dusk.module.auth.enums.ELevel;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * @author kefuming
 * @CreateTime 2022-11-08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrUpdateUserInfoInput extends CreateOrUpdateUserInput {
    @NotNull
    @Schema(description = "员工级别")
    private ELevel level;

    @Schema(description = "所属厂站列表")
    private List<Long> stationIds;

    @Schema(description = "岗位")
    private String job;

    @Schema(description = "卡号")
    private String accessCard;

    @Schema(description = "入厂时间")
    private LocalDate enterDate;

    @Schema(description = "管理的组织id")
    private List<Long> managerOrgIds;
}
