package com.dusk.module.auth.dto.edition;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-08 14:51
 */
@Getter
@Setter
public class LocalizableComboboxItemSourceDto {
    private List<LocalizableComboboxItemDto> Items = new ArrayList<>();
}
