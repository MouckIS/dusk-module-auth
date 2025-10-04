package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class ZoneDetailDto extends EntityDto {

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;
    /**
     * 栏目ID
     */
    @Schema(description = "布局id")
    private String classifyId;

    /**
     * 布局方向
     */
    @Schema(description = "布局方向")
    private String orientation;

    /**
     * 位置
     */
    @Schema(description = "位置")
    private Integer zonePosition;

    /**
     * 统计项列表
     */
    @Schema(description = "统计项列表")
    private List<ZoneItemDetailDto> zoneItems;

}
