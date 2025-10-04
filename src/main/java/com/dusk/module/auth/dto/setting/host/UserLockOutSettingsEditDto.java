package com.dusk.module.auth.dto.setting.host;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-21 16:28
 */
@Schema(description = "用户锁定")
@Data
public class UserLockOutSettingsEditDto {
    @Schema(description = "登录失败后启用用户的帐户锁定")
    public boolean enabled;

    @Schema(description = "在锁定帐户之前的累计登录失败的最大数量")
    public int maxFailedAccessAttemptsBeforeLockout;

    @Schema(description = "帐户锁定持续时间（秒）")
    public int defaultAccountLockoutSeconds;
}
