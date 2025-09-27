package com.dusk.module.ddm.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2021/7/27
 */
@Data
@ApiModel(description = "大类关联模块数据")
public class RelationModuleInput {
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

    /**
     * 布局位置
     */
    @ApiModelProperty("布局位置")
    private Integer layoutLocation;
}
