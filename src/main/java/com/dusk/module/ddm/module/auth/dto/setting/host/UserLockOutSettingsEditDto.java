package com.dusk.module.ddm.module.auth.dto.setting.host;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-21 16:28
 */
@ApiModel("用户锁定")
@Data
public class UserLockOutSettingsEditDto {
    @ApiModelProperty("登录失败后启用用户的帐户锁定")
    public boolean enabled;

    @ApiModelProperty("在锁定帐户之前的累计登录失败的最大数量")
    public int maxFailedAccessAttemptsBeforeLockout;

    @ApiModelProperty("帐户锁定持续时间（秒）")
    public int defaultAccountLockoutSeconds;
}
