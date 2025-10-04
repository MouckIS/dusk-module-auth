package com.dusk.module.auth.dto.setting.tenants;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-06-16 8:10
 */
@Data
@Schema(description = "发票")
public class TenantBillingSettingsEditDto{
    @Schema(description = "法定名称")
    public String legalName;

    @Schema(description = "地址")
    public String address;

    @Schema(description = "税务代码")
    private String taxVatNo;
}

