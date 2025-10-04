package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020/5/18 11:18
 */
@Data
public class SendVerificationCodeForUpdatingMobileOutput {
    @Schema(description = "验证码")
    public String verificationCode;
}
