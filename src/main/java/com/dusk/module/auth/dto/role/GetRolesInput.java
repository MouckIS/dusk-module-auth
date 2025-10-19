package com.dusk.module.auth.dto.role;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@ApiModel
public class GetRolesInput extends PagedAndSortedInputDto {
    @ApiModelProperty("根据角色名模糊查找")
    private String filter;
}
