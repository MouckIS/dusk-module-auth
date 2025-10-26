package com.dusk.module.auth.dto.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-12-16 8:27
 */
@Getter
@Setter
public class LoginInfoDto {
    @Schema(description = "是否需要验证码")
    private boolean needCaptcha;
}
