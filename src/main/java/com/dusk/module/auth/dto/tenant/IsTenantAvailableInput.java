package com.dusk.module.auth.dto.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-18 14:26
 */
@Data
public class IsTenantAvailableInput {
    @Schema(description = "租户代码")
    private String tenantName;
}
