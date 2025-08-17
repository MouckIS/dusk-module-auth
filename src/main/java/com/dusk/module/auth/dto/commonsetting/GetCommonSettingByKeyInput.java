package com.dusk.module.auth.dto.commonsetting;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-18 11:02
 */
@Data
public class GetCommonSettingByKeyInput {
    @ApiModelProperty("key")
    private String key;
}
