package com.dusk.module.auth.dto.setting.tenants;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-06-16 8:21
 */
@Data
public class TenantTicketSettingsEditDto {
    @ApiModelProperty("不解析挂拆牌术语")
    private boolean unAnalyzeDeviceBoardTerm;
    
    @ApiModelProperty("过滤备品备件")
    private boolean filterSpareParts;
}
