package com.dusk.module.auth.dto.commonsetting;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.EntityDto;

import javax.validation.constraints.NotBlank;

/**
 * @author kefuming
 * @date 2020-05-18 10:56
 */
@Data
public class CreateOrUpdateCommonSettingInput extends EntityDto {
    /**
     * key
     */
    @NotBlank(message = "key不能为空")
    @ApiModelProperty("key")
    private String key;

    /**
     * value
     */
    @NotBlank(message = "value不能为空")
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
