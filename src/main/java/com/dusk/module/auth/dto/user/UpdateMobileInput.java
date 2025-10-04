package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020/5/18 11:22
 */
@Data
public class UpdateMobileInput {
    @Schema(description = "新手机号")
    public String newMobile;

    @Schema(description = "验证码")
    public String verificationCode;
}
