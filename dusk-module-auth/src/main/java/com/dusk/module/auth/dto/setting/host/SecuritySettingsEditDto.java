package com.dusk.module.auth.dto.setting.host;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-21 15:47
 */
@Getter
@Setter
public class SecuritySettingsEditDto {
    public boolean useDefaultPasswordComplexitySettings;

    public PasswordComplexitySetting passwordComplexity;

    public PasswordComplexitySetting defaultPasswordComplexity;

    public UserLockOutSettingsEditDto userLockOut;

    public TwoFactorLoginSettingsEditDto twoFactorLogin;
}
