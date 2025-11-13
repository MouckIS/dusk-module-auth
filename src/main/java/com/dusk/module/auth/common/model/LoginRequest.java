package com.dusk.module.auth.common.model;

import com.dusk.module.auth.dto.captcha.CaptchaInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 登陆输入模型
 *
 * @author kefuming
 * @date 2020-04-23 9:00
 */
@Getter
@Setter
public class LoginRequest extends CaptchaInputDto implements Serializable {
    @Schema(description = "帐户名")
    @NotBlank(message = "帐户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String password;
}
