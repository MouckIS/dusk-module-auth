package com.dusk.module.auth.dto.setting.host;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-21 16:30
 */
@Getter
@Setter
@Schema(description = "两步认证登录")
public class TwoFactorLoginSettingsEditDto {
    @Schema(description = "启用两步认证登录")
    public boolean enabled;
    @Schema(description = "enabledForApplication")
    public boolean enabledForApplication;
    @Schema(description = "启用电子邮件验证")
    public boolean emailProviderEnabled;
    @Schema(description = "启用短信验证")
    public boolean smsProviderEnabled;
    @Schema(description = "允许记住浏览器。如果您允许，用户可以选择记住浏览器，以跳过在同一个浏览器中的再次两步认证登录")
    public boolean rememberBrowserEnabled;
    @Schema(description = "启用Google身份验证器")
    public boolean googleAuthenticatorEnabled;
}
