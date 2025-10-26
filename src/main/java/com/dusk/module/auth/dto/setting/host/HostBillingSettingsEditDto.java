package com.dusk.module.auth.dto.setting.host;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-21 15:47
 */
@Getter
@Setter
@Schema(description = "发票")
public class HostBillingSettingsEditDto {
    @Schema(description = "法定名称")
    public String legalName;

    @Schema(description = "地址")
    public String address;
}
