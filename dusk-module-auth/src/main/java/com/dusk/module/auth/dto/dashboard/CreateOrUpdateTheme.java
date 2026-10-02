package com.dusk.module.auth.dto.dashboard;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Getter
@Setter
public class CreateOrUpdateTheme extends EntityDto {
    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    @Schema(description = "主题名称")
    private String name;

    /**
     * 主题描述
     */
    @NotBlank(message = "主题描述不能为空")
    @Schema(description = "主题描述")
    private String description;

    /**
     * 主题样式
     */
    @NotBlank(message = "主题样式不能为空")
    @Schema(description = "主题样式")
    private String themeType;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题")
    private String title;

    /**
     * 显示时间
     */
    @Schema(description = "是否显示时间")
    private Boolean showTime = false;

    /**
     * 显示tianq
     */
    @Schema(description = "是否显示天气")
    private Boolean showWeather = false;

    @Schema(description = "是否首页大屏")
    private Boolean mainPage = false;
}
