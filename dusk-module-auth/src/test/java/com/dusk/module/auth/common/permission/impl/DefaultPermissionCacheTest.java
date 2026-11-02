package com.dusk.module.auth.common.permission.impl;

import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.permission.RoleInfo;
import com.dusk.common.core.auth.permission.UrlPermission;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.service.IGrantPermissionService;
import com.dusk.module.auth.service.ITenantPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DefaultPermissionCache} 单元测试：覆盖匿名路径、权限定义、URL 权限
 * 的内存写入，以及 refreshAll 时把授权角色合并进 URL 权限。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultPermissionCacheTest {

    @Mock
    private IGrantPermissionRepository grantPermissionRepository;
    @Mock
    private IGrantPermissionService grantPermissionService;
    @Mock
    private ITenantPermissionService tenantPermissionService;

    private DefaultPermissionCache cache;

    @BeforeEach
    void setUp() {
        cache = new DefaultPermissionCache();
        ReflectionTestUtils.setField(cache, "grantPermissionRepository", grantPermissionRepository);
        ReflectionTestUtils.setField(cache, "grantPermissionService", grantPermissionService);
        ReflectionTestUtils.setField(cache, "tenantPermissionService", tenantPermissionService);
    }

    @Test
    @DisplayName("匿名路径按应用写入内存 Map")
    void addAnonymousPath() {
        cache.addAnonymousPath("app", List.of("/login", "/error"));

        assertThat(cache.getAllowAnonymousPath()).containsEntry("app", List.of("/login", "/error"));
    }

    @Test
    @DisplayName("权限定义按应用写入内存 Map")
    void addDefinitionPermission() {
        Permission permission = new Permission("p", "p", MultiTenancySides.All);

        cache.addDefinitionPermission("app", Map.of("p", permission));

        assertThat(cache.getDefinitionPermission().get("app")).containsEntry("p", permission);
    }

    @Test
    @DisplayName("addUrlPermission 写入后立即刷新授权角色")
    void addUrlPermissionRefreshesRoles() {
        UrlPermission urlPermission = new UrlPermission("p", "p", MultiTenancySides.All);
        Map<String, List<UrlPermission>> applicationPermission = Map.of("/api/x", List.of(urlPermission));

        RoleInfo hostRole = new RoleInfo();
        hostRole.setRoleCode("host");
        when(grantPermissionService.getAll()).thenReturn(Map.of("p", List.of(hostRole)));
        when(tenantPermissionService.getAllTenantPermission()).thenReturn(Map.of());

        cache.addUrlPermission("app", applicationPermission);

        assertThat(cache.getUrlPermission().get("app")).isSameAs(applicationPermission);
        assertThat(urlPermission.getRoles()).containsExactly(hostRole);
    }

    @Test
    @DisplayName("refreshAll：URL 权限为空时仍会读取授权数据")
    void refreshAllWithEmptyUrlPermissions() {
        cache.addDefinitionPermission("app", Map.of());

        cache.refreshAll();

        verify(grantPermissionService).getAll();
        verify(tenantPermissionService).getAllTenantPermission();
    }

    @Test
    @DisplayName("refreshAll：租户角色按 tenantId + 权限名合并")
    void refreshAllMergesTenantRoles() {
        UrlPermission urlPermission = new UrlPermission("p", "p", MultiTenancySides.All);
        Map<String, List<UrlPermission>> applicationPermission = new HashMap<>();
        applicationPermission.put("/api/x", List.of(urlPermission));

        RoleInfo tenantRole = new RoleInfo();
        tenantRole.setRoleCode("tenant");
        when(grantPermissionService.getAll()).thenReturn(Map.of("1p", List.of(tenantRole)));
        when(tenantPermissionService.getAllTenantPermission()).thenReturn(Map.of("p", List.of(1L)));

        cache.addUrlPermission("app", applicationPermission);

        cache.refreshAll();

        assertThat(urlPermission.getTenants()).containsExactly(1L);
        assertThat(urlPermission.getRoles()).extracting(RoleInfo::getRoleCode).containsExactly("tenant");
    }
}
