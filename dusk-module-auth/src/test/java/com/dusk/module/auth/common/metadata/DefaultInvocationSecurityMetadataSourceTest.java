package com.dusk.module.auth.common.metadata;

import com.dusk.common.core.auth.permission.RoleInfo;
import com.dusk.common.core.auth.permission.UrlPermission;
import com.dusk.common.core.constant.AuthConstant;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link DefaultInvocationSecurityMetadataSource} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultInvocationSecurityMetadataSourceTest {

    @Mock
    private IAuthPermissionManager authPermissionManager;

    private DefaultInvocationSecurityMetadataSource source;

    private static UrlPermission permission(String name, List<String> roleIds, List<Long> tenantIds) {
        UrlPermission permission = new UrlPermission();
        permission.setName(name);
        permission.setRoles(roleIds.stream().map(id -> {
            RoleInfo roleInfo = new RoleInfo();
            roleInfo.setId(id);
            return roleInfo;
        }).toList());
        permission.setTenants(tenantIds);
        return permission;
    }

    @BeforeEach
    void setUp() {
        source = new DefaultInvocationSecurityMetadataSource();
        ReflectionTestUtils.setField(source, "authPermissionManager", authPermissionManager);
    }

    @Test
    @DisplayName("getAttributes：应用权限配置为 null 时返回空集合")
    void getAttributes_nullApplicationPermissions() {
        when(authPermissionManager.getApplicationPermissions("app")).thenReturn(null);

        assertThat(source.getAttributes("app", "/api/user")).isEmpty();
    }

    @Test
    @DisplayName("getAttributes：URL 直接命中时展开角色、租户管理员与宿主管理员")
    void getAttributes_directMatch() {
        UrlPermission permission = permission("/api/user.list", List.of("1", "2"), List.of(5L, 6L));
        Map<String, List<UrlPermission>> permissions = new LinkedHashMap<>();
        permissions.put("/api/user.list", List.of(permission));
        when(authPermissionManager.getApplicationPermissions("app")).thenReturn(permissions);

        Collection<String> attributes = source.getAttributes("app", "/api/user.list");

        assertThat(attributes).containsExactly(
                AuthConstant.TYPE_ROLE + "1",
                AuthConstant.TYPE_ROLE + "2",
                AuthConstant.ROLE_TENANT_ADMIN + 5,
                AuthConstant.ROLE_TENANT_ADMIN + 6,
                AuthConstant.ROLE_HOST_ADMIN);
    }

    @Test
    @DisplayName("getAttributes：直接命中失败时用 Ant 模式匹配动态路由")
    void getAttributes_antPatternMatch() {
        Map<String, List<UrlPermission>> permissions = new LinkedHashMap<>();
        permissions.put("/other/*", List.of(permission("/other", List.of("9"), List.of())));
        permissions.put("/dynamic/*", List.of(permission("/dynamic", List.of("3"), List.of(7L))));
        when(authPermissionManager.getApplicationPermissions("app")).thenReturn(permissions);

        Collection<String> attributes = source.getAttributes("app", "/dynamic/42");

        assertThat(attributes).containsExactly(
                AuthConstant.TYPE_ROLE + "3",
                AuthConstant.ROLE_TENANT_ADMIN + 7,
                AuthConstant.ROLE_HOST_ADMIN);
    }

    @Test
    @DisplayName("getAttributes：直接命中与 Ant 匹配都失败时返回空集合")
    void getAttributes_noMatch() {
        Map<String, List<UrlPermission>> permissions = new LinkedHashMap<>();
        permissions.put("/known/*", List.of(permission("/known", List.of("1"), List.of())));
        when(authPermissionManager.getApplicationPermissions("app")).thenReturn(permissions);

        assertThat(source.getAttributes("app", "/unknown")).isEmpty();
    }

    @Test
    @DisplayName("getAttributes：租户列表为 null 时只生成角色与宿主管理员")
    void getAttributes_nullTenants() {
        UrlPermission permission = permission("/api/user", List.of("1"), null);
        when(authPermissionManager.getApplicationPermissions("app"))
                .thenReturn(Map.of("/api/user", List.of(permission)));

        Collection<String> attributes = source.getAttributes("app", "/api/user");

        assertThat(attributes).containsExactly(AuthConstant.TYPE_ROLE + "1", AuthConstant.ROLE_HOST_ADMIN);
    }

    @Test
    @DisplayName("getAttributes：命中但权限列表为空时返回空集合")
    void getAttributes_emptyPermissionList() {
        when(authPermissionManager.getApplicationPermissions("app"))
                .thenReturn(Map.of("/api/user", List.of()));

        assertThat(source.getAttributes("app", "/api/user")).isEmpty();
    }

    @Test
    @DisplayName("getAttributes：权限没有角色时仍生成宿主管理员")
    void getAttributes_noRoles() {
        UrlPermission permission = permission("/api/user", List.of(), List.of());
        when(authPermissionManager.getApplicationPermissions("app"))
                .thenReturn(Map.of("/api/user", List.of(permission)));

        assertThat(source.getAttributes("app", "/api/user"))
                .containsExactly(AuthConstant.ROLE_HOST_ADMIN);
    }
}
