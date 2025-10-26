package com.dusk.module.auth.dto.dashboard;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Getter
@Setter
public class ThemeDetailDto extends EntityDto {
    /**
     * 主题名称
     */
    @Schema(description = "主题名称")
    private String name;
    /**
     * 主题描述
     */
    @Schema(description = "主题描述")
    private String description;
    /**
     * 主题样式
     */
    @Schema(description = "主题样式")
    private String themeType;

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    @Schema(description = "是否首页大屏")
    private Boolean mainPage;
    /**
     * 显示时间
     */
    @Schema(description = "显示时间")
    private Boolean showTime;

    /**
     * 显示天气
     */
    @Schema(description = "显示天气")
    private Boolean showWeather;

    /**
     * 大类列表
     */
    @Schema(description = "大类列表")
    private List<ClassifyDetailDto> classifyList;
}
