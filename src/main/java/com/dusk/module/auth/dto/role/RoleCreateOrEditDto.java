package com.dusk.module.auth.dto.role;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.EntityDto;


import jakarta.validation.constraints.NotBlank;

@ApiModel
@Data
public class RoleCreateOrEditDto extends EntityDto {

    //code
    @NotBlank
    @ApiModelProperty("角色代码")
    private String roleCode;
    //name
    @NotBlank
    @ApiModelProperty("角色名称")
    private String roleName;

    @ApiModelProperty("版本号")
    private int version;

    @ApiModelProperty("是否是默认权限")
    private boolean isDefault;

//    @ApiModelProperty("权限列表")
//    private List<CreateOrEditRolePermissionDto>  permissions;

//    public RoleCreateOrEditDto(){
//        permissions = new ArrayList<>();
//    }
}
