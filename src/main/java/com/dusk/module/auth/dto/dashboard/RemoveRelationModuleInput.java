package com.dusk.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2021/7/27
 */
@Getter
@Setter
@ApiModel(description = "删除大类关联模块数据")
public class RemoveRelationModuleInput {
    /**
     * 分类Id
     */
    @ApiModelProperty("大类Id")
    private Long classifyId;

    /**
     * 模块id
     */
    @ApiModelProperty("模块id")
    private Long moduleId;
}
