package com.dusk.module.auth.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author 王吉
 * @date 2021-08-04 9:23
 */
@Getter
@Setter
@Schema
public class RolePermissionDto implements Serializable {
    //@Mapping(value = "parent.name")
    private String parentName;
    private String name;
    private String displayName;
    private boolean granted;
}
