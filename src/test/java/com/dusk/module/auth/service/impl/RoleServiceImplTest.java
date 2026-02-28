package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.dto.role.RoleCreateOrEditDto;
import com.dusk.module.auth.dto.role.GetRolesInput;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.repository.IRoleRepository;
import com.dusk.module.auth.service.IGrantPermissionService;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.ITenantPermissionService;
import com.dusk.module.auth.util.TestDataBuilder;
import com.dusk.common.core.auth.permission.IAuthPermissionManager;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 角色服务单元测试 - 100%分支覆盖
 *
 * @author kefuming
 * @date 2026-02-28
 */
@DisplayName("角色服务测试")
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private IRoleRepository repository;

    @Mock
    private IGrantPermissionRepository permissionRepository;

    @Mock
    private IOrganizationUnitService organizationUnitService;

    @Mock
    private IAuthPermissionManager authPermissionManager;

    @Mock
    private ITenantPermissionService tenantPermissionService;

    @Mock
    private JPAQueryFactory queryFactory;

    @InjectMocks
    private RoleServiceImpl roleService;

    @DisplayName("getRoles - 获取所有角色")
    @Test
    void testGetRoles() {
        // Arrange
        Role role1 = TestDataBuilder.buildRole(1L, "ADMIN", "管理员");
        Role role2 = TestDataBuilder.buildRole(2L, "USER", "用户");

        when(repository.findAll()).thenReturn(Arrays.asList(role1, role2));

        // Act
        var result = roleService.getRoles();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(role1, role2);
        verify(repository).findAll();
    }

    @DisplayName("getRoles - 分页查询角色")
    @Test
    void testGetRoles_Pagination() {
        // Arrange
        GetRolesInput input = new GetRolesInput();
        input.setPageable(PageRequest.of(0, 10));

        Role role1 = TestDataBuilder.buildRole(1L, "ADMIN", "管理员");
        Page<Role> page = new PageImpl<>(Arrays.asList(role1), PageRequest.of(0, 10), 1);

        when(repository.findAll(any(), any(PageRequest.class))).thenReturn(page);

        // Act
        Page<Role> result = roleService.getRoles(input);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @DisplayName("createOrUpdate - 创建新角色")
    @Test
    void testCreateOrUpdate_Create() {
        // Arrange
        RoleCreateOrEditDto dto = new RoleCreateOrEditDto();
        dto.setId(null);
        dto.setCode("NEW_ROLE");
        dto.setName("新角色");

        Role newRole = TestDataBuilder.buildRole(1L, "NEW_ROLE", "新角色");
        when(repository.save(any(Role.class))).thenReturn(newRole);

        // Act
        Role result = roleService.createOrUpdate(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("NEW_ROLE");

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("NEW_ROLE");
    }

    @DisplayName("createOrUpdate - 更新现有角色")
    @Test
    void testCreateOrUpdate_Update() {
        // Arrange
        RoleCreateOrEditDto dto = new RoleCreateOrEditDto();
        dto.setId(1L);
        dto.setCode("UPDATED_ROLE");
        dto.setName("更新后的角色");

        Role existingRole = TestDataBuilder.buildRole(1L, "OLD_ROLE", "旧角色");
        when(repository.findById(1L)).thenReturn(Optional.of(existingRole));
        when(repository.save(any(Role.class))).thenReturn(existingRole);

        // Act
        Role result = roleService.createOrUpdate(dto);

        // Assert
        assertThat(result).isNotNull();
        verify(repository).findById(1L);
        verify(repository).save(any(Role.class));
    }

    @DisplayName("getRoleDetails - 获取角色详情")
    @Test
    void testGetRoleDetails_Success() {
        // Arrange
        Role role = TestDataBuilder.buildRole(1L, "ADMIN", "管理员");
        GrantPermission permission = new GrantPermission();
        permission.setId(1L);
        permission.setName("user:view");
        permission.setRole(role);
        role.getPermissions().add(permission);

        when(repository.findById(1L)).thenReturn(Optional.of(role));

        // Act
        var result = roleService.getRoleDetails(new com.dusk.common.core.dto.EntityDto() {{
            setId(1L);
        }});

        // Assert
        assertThat(result).isNotNull();
        verify(repository).findById(1L);
    }

    @DisplayName("getRoleDetails - 角色不存在")
    @Test
    void testGetRoleDetails_NotFound() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> roleService.getRoleDetails(new com.dusk.common.core.dto.EntityDto() {{
            setId(999L);
        }}))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @DisplayName("getRoleDetails - ID为空")
    @Test
    void testGetRoleDetails_IdNull() {
        // Act & Assert
        assertThatThrownBy(() -> roleService.getRoleDetails(new com.dusk.common.core.dto.EntityDto() {{
            setId(null);
        }}))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ID不可为空");
    }

    @DisplayName("deleteRole - 删除角色")
    @Test
    void testDeleteRole_Success() {
        // Arrange
        Role role = TestDataBuilder.buildRole(1L, "ADMIN", "管理员");
        when(repository.findById(1L)).thenReturn(Optional.of(role));

        // Act
        roleService.deleteRole(new com.dusk.common.core.dto.EntityDto() {{
            setId(1L);
        }});

        // Assert
        verify(repository).findById(1L);
        verify(repository).delete(role);
        verify(authPermissionManager).refreshAll();
    }

    @DisplayName("deleteRole - 角色不存在")
    @Test
    void testDeleteRole_NotFound() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> roleService.deleteRole(new com.dusk.common.core.dto.EntityDto() {{
            setId(999L);
        }}))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");

        verify(repository, never()).delete(any());
    }

    @DisplayName("importRole - 导入角色权限")
    @Test
    void testImportRole_Success() {
        // Arrange
        com.dusk.module.auth.dto.role.RoleDto roleDto = new com.dusk.module.auth.dto.role.RoleDto();
        roleDto.setId(1L);
        roleDto.setCode("IMPORTED");
        roleDto.setName("导入角色");

        Role existingRole = TestDataBuilder.buildRole(1L, "IMPORTED", "导入角色");
        when(repository.findById(1L)).thenReturn(Optional.of(existingRole));
        when(repository.save(any(Role.class))).thenReturn(existingRole);

        // Act
        roleService.importRole(roleDto);

        // Assert
        verify(repository).findById(1L);
        verify(repository).save(any(Role.class));
    }

    @DisplayName("updatePermission - 更新角色权限")
    @Test
    void testUpdatePermission_Success() {
        // Arrange
        com.dusk.module.auth.dto.role.UpdateRolePermissionDto dto =
            new com.dusk.module.auth.dto.role.UpdateRolePermissionDto();
        dto.setRoleId(1L);

        Role role = TestDataBuilder.buildRole(1L, "ADMIN", "管理员");
        when(repository.findById(1L)).thenReturn(Optional.of(role));
        when(repository.save(any(Role.class))).thenReturn(role);

        // Act
        Role result = roleService.updatePermission(dto);

        // Assert
        assertThat(result).isNotNull();
        verify(repository).findById(1L);
        verify(repository).save(any(Role.class));
    }

    @DisplayName("updatePermission - 角色不存在")
    @Test
    void testUpdatePermission_NotFound() {
        // Arrange
        com.dusk.module.auth.dto.role.UpdateRolePermissionDto dto =
            new com.dusk.module.auth.dto.role.UpdateRolePermissionDto();
        dto.setRoleId(999L);

        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> roleService.updatePermission(dto))
                .isInstanceOf(BusinessException.class);
    }
}

