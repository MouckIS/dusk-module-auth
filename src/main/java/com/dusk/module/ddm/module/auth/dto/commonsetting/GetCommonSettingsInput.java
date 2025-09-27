package com.dusk.module.ddm.module.auth.dto.commonsetting;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.PagedAndSortedInputDto;

/**
 * @author kefuming
 * @date 2020-05-18 11:13
 */
@Data
public class GetCommonSettingsInput extends PagedAndSortedInputDto {
    @ApiModelProperty("key")
    private String key;

    @ApiModelProperty("分组名")
    private String groupName;
}
