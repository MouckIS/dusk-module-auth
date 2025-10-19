package com.dusk.module.auth.dto.dashboard;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2021/7/27
 */
@Getter
@Setter
@ApiModel(description = "统计模块查询条件数据")
public class GetThemeInput extends PagedAndSortedInputDto {
    @ApiModelProperty("主题名称")
    private String name;

    @ApiModelProperty("主题标题")
    private String title;

    @ApiModelProperty("主题样式")
    private String themeType;
}
