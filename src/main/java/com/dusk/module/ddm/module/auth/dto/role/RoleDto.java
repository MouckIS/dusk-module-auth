package com.dusk.module.ddm.module.auth.dto.role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.dozermapper.core.Mapping;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.module.auth.dto.role.RolePermissionDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel
public class RoleDto extends EntityDto {
    @ApiModelProperty("角色代码")
    private String roleCode;
    @ApiModelProperty("角色名称")
    private String roleName;
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
    @ApiModelProperty("创建人名称")
    @Mapping("createUser.name")
    private String createUserName;
    @ApiModelProperty("是否是默认权限")
    private boolean isDefault;
    @JsonIgnore
    private String isDefaultStr;

    @ApiModelProperty("权限列表")
    private List<RolePermissionDto> permissionList;

    public RoleDto(){
        permissionList = new ArrayList<>();
    }

    public void addPermission(RolePermissionDto p){
        permissionList.add(p);
    }

    public String getIsDefaultStr() {
        if(isDefaultStr==null){
            isDefaultStr = isDefault?"是":"否";
        }
        return isDefaultStr;
    }
}
