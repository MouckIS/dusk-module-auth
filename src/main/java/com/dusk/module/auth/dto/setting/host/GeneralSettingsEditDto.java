package com.dusk.module.auth.dto.setting.host;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-06-15 17:30
 */
@Getter
@Setter
@Schema(description = "基本信息")
public class GeneralSettingsEditDto {
    @Schema(description = "时区")
    private String timezone;
    @Schema(description = "used for comparing user's timezone to default timezone")
    private String timezoneForComparison;
}
