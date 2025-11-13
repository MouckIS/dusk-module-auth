package com.dusk.module.auth.dto.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-18 14:28
 */
@Getter
@Setter
@NoArgsConstructor
public class IsTenantAvailableOutput {
    @Schema(description = "租户状态")
    public TenantAvailabilityState state;

    @Schema(description = "租户id")
    public Long tenantId;

    public IsTenantAvailableOutput(TenantAvailabilityState state) {
        this.state = state;
    }

    public IsTenantAvailableOutput(TenantAvailabilityState state, Long tenantId) {
        this.state = state;
        this.tenantId = tenantId;
    }
}
