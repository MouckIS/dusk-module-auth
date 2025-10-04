package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "模块")
    private DashboardModule module;

    private Long moduleItemId;

    /**
     * 统计项ID
     */
    @Schema(description = "统计项")
    private DashboardModuleItem moduleItem;

    /**
     * 区域ID
     */
    @Schema(description = "区域ID")
    private Long zoneId;

    /**
     * 顺序
     */
    @Schema(description = "顺序")
    private Integer seq;

    /**
     * 图表颜色
     */
    @Schema(description = "图表颜色")
    private String chartColor;

    /**
     * 接线图id
     */
    @Schema(description = "接线图id")
    private Long graphId;
}
