package com.dusk.module.auth.dto.setting.tenants;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-06-16 8:10
 */
@Getter
@Setter
@ApiModel("发票")
public class TenantBillingSettingsEditDto{
    @ApiModelProperty("法定名称")
    public String legalName;

    @ApiModelProperty("地址")
    public String address;

    @ApiModelProperty("税务代码")
    private String taxVatNo;
}

