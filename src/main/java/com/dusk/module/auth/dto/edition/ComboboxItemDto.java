package com.dusk.module.auth.dto.edition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author kefuming
 * @date 2020-05-08 17:56
 */
@Data
@NoArgsConstructor
public class ComboboxItemDto {
    @Schema(description = "值")
    protected String value;
    @Schema(description = "显示名")
    protected String displayText;
    @Schema(description = "是否选中")
    protected boolean isSelected;

    public ComboboxItemDto(String value, String displayText){
        this.value = value;
        this.displayText = displayText;
    }
}
