package com.dusk.module.ddm.module.auth.dto.setting.host;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-21 15:47
 */
@Data
@ApiModel("发票")
public class HostBillingSettingsEditDto {
    @ApiModelProperty("法定名称")
    public String legalName;

    @ApiModelProperty("地址")
    public String address;
}
