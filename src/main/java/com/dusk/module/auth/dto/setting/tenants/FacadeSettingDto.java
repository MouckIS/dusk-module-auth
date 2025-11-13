package com.dusk.module.auth.dto.setting.tenants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/11/17 11:35
 */
@Getter
@Setter
public class FacadeSettingDto extends FacadeSettingEditDto {
    @Schema(description = "css样式文件名")
    private String cssFileName;
    @Schema(description = "css样式文件下载地址")
    private String cssUrl;

    @Schema(description = "web登录页右上角logo文件名")
    private String webLoginPageLogoFileName;
    @Schema(description = "web登录页右上角logo下载地址")
    private String webLoginPageLogoUrl;

    @Schema(description = "web登录页背景图文件名")
    private String webLoginPageBackgroundFileName;
    @Schema(description = "web登录页背景图下载地址")
    private String webLoginPageBackgroundUrl;

    @Schema(description = "web主页右上角logo（大）文件名")
    private String webIndexPageLogoLargeFileName;
    @Schema(description = "web主页右上角logo（大）下载地址")
    private String webIndexPageLogoLargeUrl;

    @Schema(description = "web主页右上角logo（小）文件名")
    private String webIndexPageLogoMiniFileName;
    @Schema(description = "web主页右上角logo（小）下载地址")
    private String webIndexPageLogoMiniUrl;
}
