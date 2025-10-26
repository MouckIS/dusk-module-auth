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
public class TenantManagementSettingsEditDto {
    @Schema(description = "Setting_AllowSelfRegistration")
    public boolean allowSelfRegistration;

    @Schema(description = "新注册的默认项目")
    public boolean newRegisteredTenantActiveByDefault;

    @Schema(description = "用户注册时使用图片验证码(captcha).")
    public boolean useCaptchaOnRegistration;

    @Schema(description = "版本")
    public String defaultEditionId;
}
