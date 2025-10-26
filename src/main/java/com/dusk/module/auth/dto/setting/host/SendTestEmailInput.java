package com.dusk.module.auth.dto.setting.host;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-21 17:02
 */
@Getter
@Setter
public class SendTestEmailInput {
    @Schema(description = "邮箱地址")
    @NotBlank(message = "邮箱地址不能为空！")
    private String emailAddress;
}
