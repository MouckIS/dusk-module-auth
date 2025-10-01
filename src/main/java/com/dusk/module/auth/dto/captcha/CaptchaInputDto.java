package com.dusk.module.auth.dto.captcha;

import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("验证码的key")
    private String key;

    @ApiModelProperty("验证码")
    private String captcha;
}
