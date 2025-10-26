package com.dusk.module.auth.dto.role;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Schema
public class GetRolesInput extends PagedAndSortedInputDto {
    @Schema(description = "根据角色名模糊查找")
    private String filter;
}
