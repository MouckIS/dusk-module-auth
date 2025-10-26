package com.dusk.module.auth.dto.setting.host;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-21 15:47
 */
@Getter
@Setter
public class HostUserManagementSettingsEditDto {
    @Schema(description = "必须验证邮箱地址后才能登录")
    public boolean emailConfirmationRequiredForLogin;
    @Schema(description = "启用电话号码验证（通过短信）")
    public boolean smsVerificationEnabled;
}
