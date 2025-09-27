package com.dusk.module.ddm.module.auth.dto.setting.tenants;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-06-16 8:17
 */
@Data
@ApiModel("用户管理")
public class TenantUserManagementSettingsEditDto {
    @ApiModelProperty("允许用户注册")
    private boolean allowSelfRegistration;
    @ApiModelProperty("注册用户默认激活")
    private boolean newRegisteredUserActiveByDefault;
    @ApiModelProperty("必须验证邮箱地址后才能登录")
    private boolean emailConfirmationRequiredForLogin;
    @ApiModelProperty("用户注册时使用图片验证码")
    private boolean useCaptchaOnRegistration;
}
