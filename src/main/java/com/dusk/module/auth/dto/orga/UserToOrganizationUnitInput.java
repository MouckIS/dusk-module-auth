package com.dusk.module.auth.dto.orga;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-13 15:40
 */
@Data
public class UserToOrganizationUnitInput {
    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "组织机构id")
    private Long organizationUnitId;
}
