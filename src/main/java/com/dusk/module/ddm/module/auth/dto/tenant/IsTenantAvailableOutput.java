package com.dusk.module.ddm.module.auth.dto.tenant;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author kefuming
 * @date 2020-05-18 14:28
 */
@Data
@NoArgsConstructor
public class IsTenantAvailableOutput {
    @ApiModelProperty("租户状态")
    public TenantAvailabilityState state;

    @ApiModelProperty("租户id")
    public Long tenantId;

    public IsTenantAvailableOutput(TenantAvailabilityState state)
    {
        this.state = state;
    }

    public IsTenantAvailableOutput(TenantAvailabilityState state, Long tenantId)
    {
        this.state = state;
        this.tenantId = tenantId;
    }
}
