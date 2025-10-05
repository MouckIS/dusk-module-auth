package com.dusk.module.auth.dto.setting.host;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-21 15:48
 */
@Getter
@Setter
public class PushSettingEditDto {
    public String iosAppKey;

    public String androidAppKey;

    public String secret;

    public PushMobilePagesSettingEditDto pages;
}
