package com.dusk.module.auth.mapper;

import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.rpc.auth.dto.role.RolePermissionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author : kefuming
 * @date : 2025/10/8 12:49
 */
@Mapper
public interface PermissionMapper {
    PermissionMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(PermissionMapper.class);

    @Mapping(source = "parent.name", target = "parentName")
    RolePermissionDto toRolePermissionDto(Permission permission);

}
