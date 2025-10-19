package com.dusk.module.auth.dto.edition;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-08 17:58
 */
@Getter
@Setter
@NoArgsConstructor
public class SubscribableEditionComboboxItemDto extends ComboboxItemDto{
    private Boolean isFree;

    public SubscribableEditionComboboxItemDto(String value, String displayText, Boolean isFree)
    {
        super(value, displayText);
        this.isFree = isFree;
    }
}
