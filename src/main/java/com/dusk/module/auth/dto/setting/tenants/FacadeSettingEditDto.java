package com.dusk.module.auth.dto.setting.tenants;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/11/16 15:35
 */
@Getter
@Setter
public class FacadeSettingEditDto {
    @ApiModelProperty("css样式文件id")
    private Long cssId;

    @ApiModelProperty("web登录页右上角logo文件id")
    private Long webLoginPageLogoId;

    @ApiModelProperty("web登录页背景图文件id")
    private Long webLoginPageBackgroundId;

    @ApiModelProperty("web主页右上角logo（大）文件id")
    private Long webIndexPageLogoLargeId;

    @ApiModelProperty("web主页右上角logo（小）文件id")
    private Long webIndexPageLogoMiniId;
}

