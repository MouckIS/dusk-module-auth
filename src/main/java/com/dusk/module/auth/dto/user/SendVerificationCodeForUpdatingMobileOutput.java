package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020/5/18 11:18
 */
@Data
public class SendVerificationCodeForUpdatingMobileOutput {
    @ApiModelProperty("验证码")
    public String verificationCode;
}
