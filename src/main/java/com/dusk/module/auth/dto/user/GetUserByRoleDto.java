package com.dusk.module.auth.dto.user;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.enums.EUnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2021/8/4 16:00
 */

@Getter
@Setter
public class GetUserByRoleDto extends PagedAndSortedInputDto {
    @Schema(description = "角色id")
    @NotNull(message = "角色id不能为空")
    private Long roleId;

    @Schema(description = "过滤用户名")
    private String userName;

    @Schema(description = "用户类型")
    private EUnitType userType;
}
