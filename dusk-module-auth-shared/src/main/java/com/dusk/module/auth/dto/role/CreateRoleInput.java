package com.dusk.module.auth.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Schema
public class CreateRoleInput implements Serializable {

    @Schema(description = "角色代码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

}
