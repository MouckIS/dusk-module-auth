package com.dusk.module.auth.dto.setting.tenants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-06-16 8:21
 */
@Getter
@Setter
public class TenantTicketSettingsEditDto {
    @Schema(description = "不解析挂拆牌术语")
    private boolean unAnalyzeDeviceBoardTerm;

    @Schema(description = "过滤备品备件")
    private boolean filterSpareParts;
}
