package com.dusk.module.auth.dto.role;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema
public class UpdateRolePermissionDto extends EntityDto {
    @Schema(description = "权限列表")
    private List<CreateOrEditRolePermissionDto> permissions;

    public UpdateRolePermissionDto(){
        permissions = new ArrayList<>();
    }
}
