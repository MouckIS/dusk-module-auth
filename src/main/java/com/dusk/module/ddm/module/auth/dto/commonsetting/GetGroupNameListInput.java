package com.dusk.module.ddm.module.auth.dto.commonsetting;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-18 13:42
 */
@Data
public class GetGroupNameListInput {
    @ApiModelProperty("根据分组名进行模糊查找")
    private String filter;
}
