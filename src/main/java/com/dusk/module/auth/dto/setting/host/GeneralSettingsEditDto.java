package com.dusk.module.auth.dto.setting.host;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-06-15 17:30
 */
@Getter
@Setter
@ApiModel("基本信息")
public class GeneralSettingsEditDto {
    @ApiModelProperty("时区")
    private String timezone;
    @ApiModelProperty("used for comparing user's timezone to default timezone")
    private String timezoneForComparison;
}
