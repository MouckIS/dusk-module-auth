package com.dusk.module.auth.dto.timing;

import com.dusk.module.ddm.enums.SettingScopes;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-06-16 18:49
 */
@Getter
@Setter
public class GetTimezonesInput {
    private SettingScopes defaultTimezoneScope;
}
