package com.dusk.module.auth.dto.configuration;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-12-16 8:27
 */
@Getter
@Setter
public class LoginInfoDto {
    @ApiModelProperty("是否需要验证码")
    private boolean needCaptcha;
}
