package com.dusk.module.ddm.module.auth.dto.edition;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-08 14:51
 */
@Data
public class LocalizableComboboxItemSourceDto {
    private List<LocalizableComboboxItemDto> Items = new ArrayList<>();
}
