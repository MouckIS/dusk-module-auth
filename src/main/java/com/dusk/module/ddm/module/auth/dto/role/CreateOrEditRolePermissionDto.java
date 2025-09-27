package com.dusk.module.ddm.module.auth.dto.role;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel
@Data
public class CreateOrEditRolePermissionDto implements Serializable {
    private String name;
}
