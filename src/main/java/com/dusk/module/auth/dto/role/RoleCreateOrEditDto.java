package com.dusk.module.auth.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;
import lombok.Getter;
import lombok.Setter;


import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Schema
public class RoleCreateOrEditDto extends EntityDto {

    //code
    @NotBlank
    @Schema(description = "角色代码")
    private String roleCode;
    //name
    @NotBlank
    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "版本号")
    private int version;

    @Schema(description = "是否是默认权限")
    private boolean isDefault;

//    @Schema(description = "权限列表")
//    private List<CreateOrEditRolePermissionDto>  permissions;

//    public RoleCreateOrEditDto(){
//        permissions = new ArrayList<>();
//    }
}
