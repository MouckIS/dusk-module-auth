package com.dusk.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.EntityDto;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class ClassifyDetailDto extends EntityDto {
    /**
     * 名称
     */
    @ApiModelProperty("名称")
    private String name;
    /**
     * 布局id
     */
    @ApiModelProperty("布局id")
    private String layoutId;

    /**
     * 区域总数
     */
    @ApiModelProperty("区域总数")
    private int zoneNum;

    /**
     * 主题Id
     */
    @ApiModelProperty("主题Id")
    private Long themeId;

    /**
     * 顺序
     */
    @ApiModelProperty("顺序")
    private Integer seq;

    @ApiModelProperty("区域列表")
    private List<ZoneDetailDto> zones;
}
