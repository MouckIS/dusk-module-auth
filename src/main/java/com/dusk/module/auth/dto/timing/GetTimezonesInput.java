package com.dusk.module.auth.dto.timing;

import lombok.Data;
import com.dusk.common.framework.setting.SettingScopes;

/**
 * @author kefuming
 * @date 2020-06-16 18:49
 */
@Data
public class GetTimezonesInput {
    private SettingScopes defaultTimezoneScope;
}
