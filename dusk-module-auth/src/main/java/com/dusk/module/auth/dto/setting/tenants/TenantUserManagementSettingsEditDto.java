package com.dusk.module.auth.dto.setting.tenants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-06-16 8:17
 */
@Getter
@Setter
@Schema(description = "用户管理")
public class TenantUserManagementSettingsEditDto {
    @Schema(description = "允许用户注册")
    private boolean allowSelfRegistration;
    @Schema(description = "注册用户默认激活")
    private boolean newRegisteredUserActiveByDefault;
    @Schema(description = "必须验证邮箱地址后才能登录")
    private boolean emailConfirmationRequiredForLogin;
    @Schema(description = "用户注册时使用图片验证码")
    private boolean useCaptchaOnRegistration;
}
