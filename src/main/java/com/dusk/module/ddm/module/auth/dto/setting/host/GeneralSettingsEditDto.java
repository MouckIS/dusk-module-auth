package com.dusk.module.ddm.module.auth.dto.setting.host;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-06-15 17:30
 */
@Data
@ApiModel("基本信息")
public class GeneralSettingsEditDto {
    @ApiModelProperty("时区")
    private String timezone;
    @ApiModelProperty("used for comparing user's timezone to default timezone")
    private String timezoneForComparison;
}
