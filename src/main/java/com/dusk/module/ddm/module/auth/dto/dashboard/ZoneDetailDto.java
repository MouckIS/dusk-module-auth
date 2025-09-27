package com.dusk.module.ddm.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("名称")
    private String name;
    /**
     * 栏目ID
     */
    @ApiModelProperty("布局id")
    private String classifyId;

    /**
     * 布局方向
     */
    @ApiModelProperty("布局方向")
    private String orientation;

    /**
     * 位置
     */
    @ApiModelProperty("位置")
    private Integer zonePosition;

    /**
     * 统计项列表
     */
    @ApiModelProperty("统计项列表")
    private List<ZoneItemDetailDto> zoneItems;

}
