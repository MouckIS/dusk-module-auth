package com.dusk.module.auth.dto.role;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.rpc.auth.dto.role.RolePermissionDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema
public class RoleDto extends EntityDto {
    @Schema(description = "角色代码")
    private String roleCode;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "创建人名称")
    //@Mapping("createUser.name")
    private String createUserName;
    @Schema(description = "是否是默认权限")
    private boolean isDefault;
    @JsonIgnore
    private String isDefaultStr;

    @Schema(description = "权限列表")
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
