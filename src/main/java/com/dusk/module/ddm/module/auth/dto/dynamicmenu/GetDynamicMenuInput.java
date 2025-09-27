package com.dusk.module.ddm.module.auth.dto.dynamicmenu;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.PagedAndSortedInputDto;

/**
 * @author jianjianhong
 * @date 2023/3/14 下午4:58
 */
@Getter
@Setter
public class GetDynamicMenuInput extends PagedAndSortedInputDto {
    @ApiModelProperty("类型")
    private String dynamicType;
    @ApiModelProperty("菜单名")
    private String name;
}
