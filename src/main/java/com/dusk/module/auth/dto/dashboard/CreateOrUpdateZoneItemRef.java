package com.dusk.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class CreateOrUpdateZoneItemRef {
    /**
     * 模块Id
     */
    @NotNull(message = "模块Id不能为空")
    @ApiModelProperty("模块Id")
    private Long moduleId;

    /**
     * 统计项ID
     */
    @NotNull(message = "统计项ID不能为空")
    @ApiModelProperty("统计项ID")
    private Long moduleItemId;

    /**
     * 顺序
     */
    @NotNull(message = "顺序不能为空")
    @ApiModelProperty("顺序")
    private Integer seq;

    /**
     * 图表颜色
     */
    @ApiModelProperty("图表颜色")
    private String chartColor;

    /**
     * 接线图id
     */
    @ApiModelProperty("接线图id")
    private Long graphId;
}
