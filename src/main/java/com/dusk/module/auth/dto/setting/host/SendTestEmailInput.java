package com.dusk.module.auth.dto.setting.host;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

/**
 * @author kefuming
 * @date 2020-05-21 17:02
 */
@Getter
@Setter
public class SendTestEmailInput {
    @ApiModelProperty("邮箱地址")
    @NotBlank(message = "邮箱地址不能为空！")
    private String emailAddress;
}
