package com.dusk.module.auth.common.permission.impl;

import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.permission.UrlPermission;
import com.dusk.module.auth.common.permission.IPermissionCache;
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
 * {@link AuthPermissionManager} 单元测试：覆盖 URL 权限查找的命中/未命中，
 * 以及 Host/Tenant 两侧权限字典过滤（{@link MultiTenancySides#All} 同时属于两侧）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthPermissionManagerTest {

    private static final String APP = "auth-app";

    @Mock
    private IPermissionCache permissionCache;

    private AuthPermissionManager manager;

    private static Permission permission(String name, MultiTenancySides side) {
        return new Permission(name, name, side);
    }

    private static UrlPermission urlPermission(String name) {
        return new UrlPermission(name, name, MultiTenancySides.All);
    }

    @BeforeEach
    void setUp() {
        manager = new AuthPermissionManager();
        ReflectionTestUtils.setField(manager, "permissionCache", permissionCache);
    }

    private void cacheDefinitions(Permission... permissions) {
        Map<String, Permission> definitions = new HashMap<>();
        for (Permission permission : permissions) {
            definitions.put(permission.getName(), permission);
        }
        when(permissionCache.getDefinitionPermission()).thenReturn(Map.of(APP, definitions));
    }

    @Test
    @DisplayName("getPermission：应用与 URL 均存在时返回权限列表")
    void getPermissionHit() {
        UrlPermission urlPermission = urlPermission("p");
        when(permissionCache.getUrlPermission())
                .thenReturn(Map.of(APP, Map.of("/api/x", List.of(urlPermission))));

        assertThat(manager.getPermission(APP, "/api/x")).containsExactly(urlPermission);
    }

    @Test
    @DisplayName("getPermission：应用存在但 URL 未配置时返回 null")
    void getPermissionUrlMiss() {
        when(permissionCache.getUrlPermission()).thenReturn(Map.of(APP, Map.of("/api/x", List.of())));

        assertThat(manager.getPermission(APP, "/api/other")).isNull();
    }

    @Test
    @DisplayName("getPermission：应用未配置时返回 null")
    void getPermissionApplicationMiss() {
        when(permissionCache.getUrlPermission()).thenReturn(Map.of());

        assertThat(manager.getPermission(APP, "/api/x")).isNull();
    }

    @Test
    @DisplayName("getApplicationPermissions：命中返回应用权限，未命中返回 null")
    void getApplicationPermissions() {
        Map<String, List<UrlPermission>> permissions = Map.of("/api/x", List.of(urlPermission("p")));
        when(permissionCache.getUrlPermission()).thenReturn(Map.of(APP, permissions));

        assertThat(manager.getApplicationPermissions(APP)).isSameAs(permissions);
        assertThat(manager.getApplicationPermissions("unknown")).isNull();
    }

    @Test
    @DisplayName("getDefinitionPermission(true)：仅返回 All/Tenant 侧权限名")
    void getDefinitionPermissionForTenantSide() {
        cacheDefinitions(
                permission("all", MultiTenancySides.All),
                permission("tenant", MultiTenancySides.Tenant),
                permission("host", MultiTenancySides.Host));

        assertThat(manager.getDefinitionPermission(true))
                .containsExactlyInAnyOrder("all", "tenant");
    }

    @Test
    @DisplayName("getDefinitionPermission(false)：仅返回 All/Host 侧权限名")
    void getDefinitionPermissionForHostSide() {
        cacheDefinitions(
                permission("all", MultiTenancySides.All),
                permission("tenant", MultiTenancySides.Tenant),
                permission("host", MultiTenancySides.Host));

        assertThat(manager.getDefinitionPermission(false))
                .containsExactlyInAnyOrder("all", "host");
    }

    @Test
    @DisplayName("getDefinitionPermissionTree(true/false)：返回对应侧的权限对象")
    void getDefinitionPermissionTree() {
        cacheDefinitions(
                permission("all", MultiTenancySides.All),
                permission("tenant", MultiTenancySides.Tenant),
                permission("host", MultiTenancySides.Host));

        assertThat(manager.getDefinitionPermissionTree(true))
                .extracting(Permission::getName)
                .containsExactlyInAnyOrder("all", "tenant");
        assertThat(manager.getDefinitionPermissionTree(false))
                .extracting(Permission::getName)
                .containsExactlyInAnyOrder("all", "host");
    }

    @Test
    @DisplayName("空权限字典时两种过滤均返回空集合")
    void emptyDefinitionPermission() {
        when(permissionCache.getDefinitionPermission()).thenReturn(Map.of());

        assertThat(manager.getDefinitionPermission(true)).isEmpty();
        assertThat(manager.getDefinitionPermissionTree(false)).isEmpty();
    }

    @Test
    @DisplayName("refreshAll 委托权限缓存刷新")
    void refreshAllDelegates() {
        manager.refreshAll();

        verify(permissionCache).refreshAll();
    }
}
