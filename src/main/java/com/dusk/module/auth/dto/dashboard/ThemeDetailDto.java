package com.dusk.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class ThemeDetailDto extends EntityDto {
    /**
     * 主题名称
     */
    @ApiModelProperty("主题名称")
    private String name;
    /**
     * 主题描述
     */
    @ApiModelProperty("主题描述")
    private String description;
    /**
     * 主题样式
     */
    @ApiModelProperty("主题样式")
    private String themeType;

    /**
     * 标题
     */
    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("是否首页大屏")
    private Boolean mainPage;
    /**
     * 显示时间
     */
    @ApiModelProperty("显示时间")
    private Boolean showTime;

    /**
     * 显示天气
     */
    @ApiModelProperty("显示天气")
    private Boolean showWeather;

    /**
     * 大类列表
     */
    @ApiModelProperty("大类列表")
    private List<ClassifyDetailDto> classifyList;
}
