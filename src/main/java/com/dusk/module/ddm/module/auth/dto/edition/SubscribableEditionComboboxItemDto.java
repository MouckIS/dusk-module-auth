package com.dusk.module.ddm.module.auth.dto.edition;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author kefuming
 * @date 2020-05-08 17:58
 */
@Data
@NoArgsConstructor
public class SubscribableEditionComboboxItemDto extends ComboboxItemDto{
    private Boolean isFree;

    public SubscribableEditionComboboxItemDto(String value, String displayText, Boolean isFree)
    {
        super(value, displayText);
        this.isFree = isFree;
    }
}
