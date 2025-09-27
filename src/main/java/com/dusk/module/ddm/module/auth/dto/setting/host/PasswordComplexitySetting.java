package com.dusk.module.ddm.module.auth.dto.setting.host;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author kefuming
 * @date 2020-05-21 16:07
 */
@Data
@EqualsAndHashCode
public class PasswordComplexitySetting {
    @ApiModelProperty("包含数字")
    public boolean requireDigit;

    @ApiModelProperty("包含小写字母")
    public boolean requireLowercase;

    @ApiModelProperty("包含特殊字符")
    public boolean requireNonAlphanumeric;

    @ApiModelProperty("包含大写字母")
    public boolean requireUppercase;

    @ApiModelProperty("最小字符长度")
    public int requiredLength;
}
