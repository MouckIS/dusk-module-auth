package com.dusk.module.auth.dto.captcha;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2020-12-21 10:09
 */
@Getter
@Setter
public class CaptchaInputDto implements Serializable {
    @Schema(description = "验证码的key")
    private String key;

    @Schema(description = "验证码")
    private String captcha;
}
