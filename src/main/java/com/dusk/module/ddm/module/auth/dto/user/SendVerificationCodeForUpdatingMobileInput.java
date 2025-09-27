package com.dusk.module.ddm.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020/5/18 11:17
 */
@Data
public class SendVerificationCodeForUpdatingMobileInput {
    @ApiModelProperty("新手机号")
    public String newMobile;
}
