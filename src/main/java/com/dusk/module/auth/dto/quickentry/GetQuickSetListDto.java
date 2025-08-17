package com.dusk.module.auth.dto.quickentry;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;

/**
 * 〈〉
 *
 * @author kefuming
 * @create 2022/2/9
 * @since 1.0.0
 */
@Getter
@Setter
public class GetQuickSetListDto extends PagedAndSortedInputDto {

    @ApiModelProperty("前端路由的name")
    private String routeName;
}
