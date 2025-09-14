package com.dusk.module.auth.dto.configuration;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.module.auth.enums.DynamicMenuOpenType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * @author kefuming
 * @date 2022-08-30 15:30
 */
@Getter
@Setter
@ApiModel
public class DynamicMenuDto {
    @ApiModelProperty("菜单名")
    private String name;
    @ApiModelProperty("类型，前端定义（低代码/动态报表/其他）")
    private String dynamicType;
    @ApiModelProperty("跳转标识，前端组装")
    private String identify;
    @ApiModelProperty("推送唯一标识，取消授权需要用到")
    private String businessKey;
    @Enumerated(EnumType.STRING)
    @ApiModelProperty("打开方式")
    private DynamicMenuOpenType type = DynamicMenuOpenType.INNER;
}
