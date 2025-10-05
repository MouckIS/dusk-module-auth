package com.dusk.module.auth.dto.role;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@ApiModel
@Getter
@Setter
public class CreateOrEditRolePermissionDto implements Serializable {
    private String name;
}
