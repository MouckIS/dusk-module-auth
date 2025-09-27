package com.dusk.module.ddm.module.auth.dto.timing;

import lombok.Data;
import com.dusk.common.core.setting.SettingScopes;

/**
 * @author kefuming
 * @date 2020-06-16 18:49
 */
@Data
public class GetTimezonesInput {
    private SettingScopes defaultTimezoneScope;
}
