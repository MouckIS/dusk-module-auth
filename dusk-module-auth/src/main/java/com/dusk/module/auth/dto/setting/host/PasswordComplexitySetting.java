package com.dusk.module.auth.dto.setting.host;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-21 16:07
 */
@Getter
@Setter
@EqualsAndHashCode
public class PasswordComplexitySetting {
    @Schema(description = "包含数字")
    public boolean requireDigit;

    @Schema(description = "包含小写字母")
    public boolean requireLowercase;

    @Schema(description = "包含特殊字符")
    public boolean requireNonAlphanumeric;

    @Schema(description = "包含大写字母")
    public boolean requireUppercase;

    @Schema(description = "最小字符长度")
    public int requiredLength;
}
