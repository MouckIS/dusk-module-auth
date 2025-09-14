package com.dusk.module.auth.common.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.module.auth.dto.captcha.CaptchaInputDto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 登陆输入模型
 *
 * @author kefuming
 * @date 2020-04-23 9:00
 */
@Data
public class LoginRequest extends CaptchaInputDto implements Serializable {
    @ApiModelProperty(value = "帐户名", position = 0)
    @NotBlank(message = "帐户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    @ApiModelProperty(value = "密码", position = 1)
    private String password;
}
