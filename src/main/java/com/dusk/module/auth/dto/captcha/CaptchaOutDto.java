package com.dusk.module.auth.dto.captcha;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-12-15 14:54
 */
@Getter
@Setter
public class CaptchaOutDto {
    @Schema(description = "获取验证码的key，登陆的时候需要返回回来")
    private String key;
    @Schema(description = "验证码图片base64")
    private String imageBase64;
}
