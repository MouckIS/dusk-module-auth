package com.dusk.module.auth.authz.version;

import com.dusk.module.auth.authz.service.AuthzEventPublisher;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.repository.IRoleRepository;
import com.dusk.module.auth.repository.ITenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link TenantPermissionInvalidationService} 单元测试：套餐变更后逐租户逐角色推进版本并发布事件。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantPermissionInvalidationServiceTest {

    @Mock
    private ITenantRepository tenantRepository;

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private AuthzPermissionVersionService permissionVersionService;

    @Mock
    private AuthzEventPublisher eventPublisher;

    private TenantPermissionInvalidationService service;

    private static Tenant tenant(Long id) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        return tenant;
    }

    private static Role role(Long id) {
        Role role = new Role();
        role.setId(id);
        return role;
    }

    @BeforeEach
    void setUp() {
        service = new TenantPermissionInvalidationService(tenantRepository, roleRepository,
                permissionVersionService, eventPublisher);
    }

    @Test
    @DisplayName("editionId 为空：不做任何失效")
    void shouldIgnoreNullEdition() {
        assertThat(service.invalidateEdition(null)).isZero();
        verifyNoInteractions(tenantRepository, roleRepository, permissionVersionService, eventPublisher);
    }

    @Test
    @DisplayName("套餐下没有租户：返回 0")
    void shouldReturnZeroWhenNoTenants() {
        when(tenantRepository.findByEdition_Id(1L)).thenReturn(List.of());

        assertThat(service.invalidateEdition(1L)).isZero();
        verifyNoInteractions(permissionVersionService, eventPublisher);
    }

    @Test
    @DisplayName("租户没有角色：不推进版本")
    void shouldSkipTenantWithoutRoles() {
        when(tenantRepository.findByEdition_Id(1L)).thenReturn(List.of(tenant(5L)));
        when(roleRepository.findByTenantId(5L)).thenReturn(List.of());

        assertThat(service.invalidateEdition(1L)).isZero();
        verify(permissionVersionService, never()).nextVersion(any(), any());
    }

    @Test
    @DisplayName("多租户多角色：逐个推进版本、发布事件并返回角色总数")
    void shouldInvalidateEveryRole() {
        when(tenantRepository.findByEdition_Id(1L)).thenReturn(List.of(tenant(5L), tenant(6L)));
        when(roleRepository.findByTenantId(5L)).thenReturn(List.of(role(10L), role(11L)));
        when(roleRepository.findByTenantId(6L)).thenReturn(List.of(role(20L)));
        when(permissionVersionService.nextVersion(5L, 10L)).thenReturn(2L);
        when(permissionVersionService.nextVersion(5L, 11L)).thenReturn(3L);
        when(permissionVersionService.nextVersion(6L, 20L)).thenReturn(4L);

        assertThat(service.invalidateEdition(1L)).isEqualTo(3);

        verify(permissionVersionService).nextVersion(5L, 10L);
        verify(permissionVersionService).nextVersion(5L, 11L);
        verify(permissionVersionService).nextVersion(6L, 20L);
        verify(eventPublisher).permissionChanged(5L, 10L, 2L);
        verify(eventPublisher).permissionChanged(5L, 11L, 3L);
        verify(eventPublisher).permissionChanged(6L, 20L, 4L);
    }
}
