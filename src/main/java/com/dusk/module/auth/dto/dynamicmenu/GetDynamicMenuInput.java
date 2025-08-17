package com.dusk.module.auth.dto.dynamicmenu;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;

/**
 * @Author jianjianhong
 * @Date 2023/3/14 下午4:58
 */
@Getter
@Setter
public class GetDynamicMenuInput extends PagedAndSortedInputDto {
    @ApiModelProperty("类型")
    private String dynamicType;
    @ApiModelProperty("菜单名")
    private String name;
}
