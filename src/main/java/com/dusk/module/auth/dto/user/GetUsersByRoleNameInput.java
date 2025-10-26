package com.dusk.module.auth.dto.user;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.enums.EUnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-12-10 9:13
 */
@Getter
@Setter
@Schema
public class GetUsersByRoleNameInput extends PagedAndSortedInputDto {
    @Schema(description = "角色名称列表")
    @NotEmpty(message = "角色名称不能为空")
    public List<String> roleNames;
    @Schema(description = "账号类型")
    private EUnitType userType;
}
