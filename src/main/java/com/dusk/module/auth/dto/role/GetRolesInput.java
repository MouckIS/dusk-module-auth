package com.dusk.module.auth.dto.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;


@Data
@ApiModel
public class GetRolesInput extends PagedAndSortedInputDto {
    @ApiModelProperty("根据角色名模糊查找")
    private String filter;
}
