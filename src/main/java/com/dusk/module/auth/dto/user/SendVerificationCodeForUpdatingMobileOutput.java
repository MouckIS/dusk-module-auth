package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/5/18 11:18
 */
@Getter
@Setter
public class SendVerificationCodeForUpdatingMobileOutput {
    @ApiModelProperty("验证码")
    public String verificationCode;
}
