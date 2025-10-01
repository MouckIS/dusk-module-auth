package com.dusk.module.auth.dto.setting.host;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-21 15:47
 */
@Data
public class TenantManagementSettingsEditDto {
    @ApiModelProperty("Setting_AllowSelfRegistration")
    public boolean allowSelfRegistration;

    @ApiModelProperty("新注册的默认项目")
    public boolean newRegisteredTenantActiveByDefault;

    @ApiModelProperty("用户注册时使用图片验证码(captcha).")
    public boolean useCaptchaOnRegistration;

    @ApiModelProperty("版本")
    public String defaultEditionId;
}
