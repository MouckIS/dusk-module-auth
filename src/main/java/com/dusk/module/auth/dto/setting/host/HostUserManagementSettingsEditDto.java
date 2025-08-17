package com.dusk.module.auth.dto.setting.host;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-21 15:47
 */
@Data
public class HostUserManagementSettingsEditDto {
    @ApiModelProperty("必须验证邮箱地址后才能登录")
    public boolean emailConfirmationRequiredForLogin;
    @ApiModelProperty("启用电话号码验证（通过短信）")
    public boolean smsVerificationEnabled;
}
