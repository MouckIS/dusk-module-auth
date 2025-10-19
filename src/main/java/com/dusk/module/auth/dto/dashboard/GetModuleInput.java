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
public class GetModuleInput extends PagedAndSortedInputDto {
    @ApiModelProperty("模块名称")
    private String name;

    @ApiModelProperty("模块编码")
    private String code;
}
