package com.dusk.module.ddm.module.auth.dto.setting.host;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-21 16:30
 */
@Data
@ApiModel("两步认证登录")
public class TwoFactorLoginSettingsEditDto {
    @ApiModelProperty("启用两步认证登录")
    public boolean enabled;
    @ApiModelProperty("enabledForApplication")
    public boolean enabledForApplication;
    @ApiModelProperty("启用电子邮件验证")
    public boolean emailProviderEnabled;
    @ApiModelProperty("启用短信验证")
    public boolean smsProviderEnabled;
    @ApiModelProperty("允许记住浏览器。如果您允许，用户可以选择记住浏览器，以跳过在同一个浏览器中的再次两步认证登录")
    public boolean rememberBrowserEnabled;
    @ApiModelProperty("启用Google身份验证器")
    public boolean googleAuthenticatorEnabled;
}
