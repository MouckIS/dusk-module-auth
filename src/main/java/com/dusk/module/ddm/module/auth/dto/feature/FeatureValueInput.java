package com.dusk.module.ddm.module.auth.dto.feature;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @version 0.0.1
 * @date 2020/4/29 14:20
 */
@Data
public class FeatureValueInput{
    /**
     * 特性名称
     */
    @ApiModelProperty(value = "特性名称")
    private String name;

    /**
     * 特性值
     */
    @ApiModelProperty(value = "特性值")
    private String value;
}
