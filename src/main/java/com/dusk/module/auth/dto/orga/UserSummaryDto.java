package com.dusk.module.auth.dto.orga;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-13 16:42
 */
@Data
public class UserSummaryDto {
    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "账号")
    private String userName;

    @Schema(description = "组织机构id")
    private String organizationUnitId;
}
