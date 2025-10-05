package com.dusk.module.auth.dto.tenant;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-18 14:26
 */
@Getter
@Setter
public class IsTenantAvailableInput {
    @ApiModelProperty("租户代码")
    private String tenantName;
}
