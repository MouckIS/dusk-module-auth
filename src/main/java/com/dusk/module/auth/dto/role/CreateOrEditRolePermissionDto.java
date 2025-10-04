package com.dusk.module.auth.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema
public class CreateOrEditRolePermissionDto implements Serializable {
    private String name;
}
