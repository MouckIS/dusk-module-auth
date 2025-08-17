package com.dusk.module.auth.dto.setting.host;

import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-21 15:48
 */
@Data
public class PushSettingEditDto {
    public String iosAppKey;

    public String androidAppKey;

    public String secret;

    public PushMobilePagesSettingEditDto pages;
}
