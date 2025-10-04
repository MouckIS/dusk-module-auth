package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020/5/18 11:17
 */
@Data
public class SendVerificationCodeForUpdatingMobileInput {
    @Schema(description = "新手机号")
    public String newMobile;
}
