package com.dusk.module.ddm.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.entity.dashboard.DashboardModule;
import com.dusk.module.auth.entity.dashboard.DashboardModuleItem;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class ZoneItemDetailDto extends EntityDto {

    private Long moduleId;
    /**
     * 模块Id
     */
    @ApiModelProperty("模块")
    private DashboardModule module;

    private Long moduleItemId;

    /**
     * 统计项ID
     */
    @ApiModelProperty("统计项")
    private DashboardModuleItem moduleItem;

    /**
     * 区域ID
     */
    @ApiModelProperty("区域ID")
    private Long zoneId;

    /**
     * 顺序
     */
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
