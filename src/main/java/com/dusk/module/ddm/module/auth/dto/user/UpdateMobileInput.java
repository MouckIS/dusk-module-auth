package com.dusk.module.ddm.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020/5/18 11:22
 */
@Data
public class UpdateMobileInput {
    @ApiModelProperty("新手机号")
    public String newMobile;

    @ApiModelProperty("验证码")
    public String verificationCode;
}
