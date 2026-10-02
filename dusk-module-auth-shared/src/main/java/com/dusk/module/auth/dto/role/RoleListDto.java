package com.dusk.module.auth.dto.role;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 王吉
 * @date 2021-08-04 9:21
 */
@Getter
@Setter
@Schema
public class RoleListDto extends EntityDto {
    @Schema(description = "角色代码")
    private String roleCode;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "是否是默认权限")
    private boolean isDefault;
    @Schema(description = "权限列表")
    private List<RolePermissionDto> permissionList;

    public RoleListDto() {
        permissionList = new ArrayList<>();
    }

    public void addPermission(RolePermissionDto p) {
        permissionList.add(p);
    }
}
