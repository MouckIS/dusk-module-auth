package com.dusk.module.auth.mapper;

import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.rpc.auth.dto.RoleSimpleDto;
import com.dusk.common.rpc.auth.dto.role.RoleListDto;
import com.dusk.common.rpc.auth.dto.role.RolePermissionDto;
import com.dusk.module.auth.dto.role.CreateOrEditRolePermissionDto;
import com.dusk.module.auth.dto.role.RoleCreateOrEditDto;
import com.dusk.module.auth.dto.role.RoleDto;
import com.dusk.module.auth.dto.user.UserRoleDto;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * @author : kefuming
 * @date : 2025/10/8 12:46
 */
@Mapper
public interface RoleMapper {
    RoleMapper INSTANCE = Mappers.getMapper(RoleMapper.class);

    Role editDtoToEntity(RoleCreateOrEditDto dto);

    RoleDto toDto(Role entity);

    RolePermissionDto entityToPermissionDto(Permission entity);

    RoleListDto enitytToListDto(Role entity);

    GrantPermission dtoToPermissionEntity(CreateOrEditRolePermissionDto rolePermissionDto);

    void updateEntityFromDto(RoleCreateOrEditDto dto, @MappingTarget Role entity);

    RoleSimpleDto listDtoToSimpleDto(RoleListDto dto);

    UserRoleDto toRoleDto(Role role);
}
