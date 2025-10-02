package com.dusk.module.auth.dto.timing;

import com.dusk.module.ddm.enums.SettingScopes;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-06-16 18:50
 */
@Data
public class GetTimezoneComboboxItemsInput {
    private SettingScopes defaultTimezoneScope;
    private String selectedTimezoneId;
}
