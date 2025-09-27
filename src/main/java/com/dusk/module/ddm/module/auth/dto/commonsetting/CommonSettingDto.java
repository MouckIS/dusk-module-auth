package com.dusk.module.ddm.module.auth.dto.commonsetting;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;

/**
 * @author kefuming
 * @date 2020-05-18 11:00
 */
@Data
public class CommonSettingDto extends EntityDto {
    /**
     * key
     */
    @ApiModelProperty("key")
    private String key;

    /**
     * value
     */
    @ApiModelProperty("value")
    private String value;

    /**
     * 分组名
     */
    @ApiModelProperty("分组名")
    private String groupName;

    /**
     * 描述
     */
    @ApiModelProperty("描述")
    private String description;
}
