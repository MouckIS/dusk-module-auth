package com.dusk.module.auth.dto.captcha;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-12-15 14:54
 */
@Getter
@Setter
public class CaptchaOutDto {
    @ApiModelProperty("获取验证码的key，登陆的时候需要返回回来")
    private String key;
    @ApiModelProperty("验证码图片base64")
    private String imageBase64;
}
