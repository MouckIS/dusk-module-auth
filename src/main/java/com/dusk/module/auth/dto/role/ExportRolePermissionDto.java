package com.dusk.module.auth.dto.role;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.dusk.common.rpc.auth.dto.role.RolePermissionDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.dozermapper.core.Mapping;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2021-08-06 9:02
 */
@Data
@ExcelIgnoreUnannotated
public class ExportRolePermissionDto extends RolePermissionDto {
    @JsonIgnore
    @Mapping("this")
    private String grantedStr;
    @JsonIgnore
    @Mapping("this")
    /**
     * 角色Excel导入导出的时候用
     */
    private List<ExportRolePermissionDto> children = new ArrayList<>();


    public ExportRolePermissionDto(RolePermissionDto rolePermissionDto) {
        setGranted(rolePermissionDto.isGranted());
        setName(rolePermissionDto.getName());
        setParentName(rolePermissionDto.getParentName());
        setDisplayName(rolePermissionDto.getDisplayName());
    }


    public String getGrantedStr() {
        return isGranted() ? "是" : "";
    }

    public void appendChild(ExportRolePermissionDto dto) {
        children.add(dto);
    }
}
