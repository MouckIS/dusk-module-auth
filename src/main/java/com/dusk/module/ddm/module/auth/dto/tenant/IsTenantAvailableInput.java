package com.dusk.module.ddm.module.auth.dto.tenant;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-18 14:26
 */
@Data
public class IsTenantAvailableInput {
    @ApiModelProperty("租户代码")
    private String tenantName;
}
