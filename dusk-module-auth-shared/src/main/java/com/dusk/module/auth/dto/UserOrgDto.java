package com.dusk.module.auth.dto;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.dto.orga.OrganizationUnitDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Author kefuming
 * @CreateTime 2022-11-16
 */
@Getter
@Setter
public class UserOrgDto extends EntityDto {
    @Schema(description = "姓名")
    private String name;

    @Schema(description = "账号")
    private String userName;

    @Schema(description = "组织架构列表")
    private List<OrganizationUnitDto> dtos;
}
