package com.dusk.module.auth.dto.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.EntityDto;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ApiModel
public class UpdateRolePermissionDto extends EntityDto {
    @ApiModelProperty("权限列表")
    private List<CreateOrEditRolePermissionDto> permissions;

    public UpdateRolePermissionDto(){
        permissions = new ArrayList<>();
    }
}
