package com.dusk.module.auth.dto.role;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

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
