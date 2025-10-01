package com.dusk.module.auth.dto.setting.host;

import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-21 15:47
 */
@Data
public class SecuritySettingsEditDto {
    public boolean useDefaultPasswordComplexitySettings;

    public PasswordComplexitySetting passwordComplexity;

    public PasswordComplexitySetting defaultPasswordComplexity;

    public UserLockOutSettingsEditDto userLockOut;

    public TwoFactorLoginSettingsEditDto twoFactorLogin;
}
