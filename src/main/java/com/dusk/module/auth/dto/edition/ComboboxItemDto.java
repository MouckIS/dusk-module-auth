package com.dusk.module.auth.dto.edition;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-08 17:56
 */
@Getter
@Setter
@NoArgsConstructor
public class ComboboxItemDto {
    @ApiModelProperty("值")
    protected String value;
    @ApiModelProperty("显示名")
    protected String displayText;
    @ApiModelProperty("是否选中")
    protected boolean isSelected;

    public ComboboxItemDto(String value, String displayText){
        this.value = value;
        this.displayText = displayText;
    }
}
