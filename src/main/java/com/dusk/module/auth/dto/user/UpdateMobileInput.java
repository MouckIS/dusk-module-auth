package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/5/18 11:22
 */
@Getter
@Setter
public class UpdateMobileInput {
    @ApiModelProperty("新手机号")
    public String newMobile;

    @ApiModelProperty("验证码")
    public String verificationCode;
}
