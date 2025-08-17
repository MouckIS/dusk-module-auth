package com.dusk.module.auth.dto.setting.tenants;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/11/17 11:35
 */
@Getter
@Setter
public class FacadeSettingDto extends FacadeSettingEditDto{
    @ApiModelProperty("css样式文件名")
    private String cssFileName;
    @ApiModelProperty("css样式文件下载地址")
    private String cssUrl;

    @ApiModelProperty("web登录页右上角logo文件名")
    private String webLoginPageLogoFileName;
    @ApiModelProperty("web登录页右上角logo下载地址")
    private String webLoginPageLogoUrl;

    @ApiModelProperty("web登录页背景图文件名")
    private String webLoginPageBackgroundFileName;
    @ApiModelProperty("web登录页背景图下载地址")
    private String webLoginPageBackgroundUrl;

    @ApiModelProperty("web主页右上角logo（大）文件名")
    private String webIndexPageLogoLargeFileName;
    @ApiModelProperty("web主页右上角logo（大）下载地址")
    private String webIndexPageLogoLargeUrl;

    @ApiModelProperty("web主页右上角logo（小）文件名")
    private String webIndexPageLogoMiniFileName;
    @ApiModelProperty("web主页右上角logo（小）下载地址")
    private String webIndexPageLogoMiniUrl;
}
