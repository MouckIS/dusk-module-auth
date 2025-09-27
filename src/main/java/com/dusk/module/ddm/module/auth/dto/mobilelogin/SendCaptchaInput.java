package com.dusk.module.ddm.module.auth.dto.mobilelogin;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.module.auth.dto.captcha.CaptchaInputDto;

import javax.validation.constraints.NotEmpty;

/**
 * @author kefuming
 * @date 2020-12-21 10:33
 */
@Getter
@Setter
public class SendCaptchaInput extends CaptchaInputDto {
    @ApiModelProperty("手机号")
    @NotEmpty(message = "手机号不能为空")
    private String mobile;
}
