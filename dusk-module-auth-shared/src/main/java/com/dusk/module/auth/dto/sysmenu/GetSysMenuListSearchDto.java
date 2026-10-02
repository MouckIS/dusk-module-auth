package com.dusk.module.auth.dto.sysmenu;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetSysMenuListSearchDto extends PagedAndSortedInputDto {
    @Schema(description = "菜单名")
    private String title;

    @Schema(description = "前端路由name")
    private String routeName;

    @Schema(description = "前端路由parentRouteName")
    private String parentRouteName;
}
