package com.dusk.module.auth.common.permission;

import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.RoleInfo;
import com.dusk.common.core.auth.permission.UrlPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PermissionUtil} 单元测试，覆盖权限字典合并的两层来源：
 * 宿主（{@code allGrantPermission[name]}）与租户（{@code allGrantPermission[tenantId + name]}）。
 */
class PermissionUtilTest {

    private static RoleInfo role(String code) {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setRoleCode(code);
        roleInfo.setRoleName(code);
        return roleInfo;
    }

    private static UrlPermission permission(String name) {
        return new UrlPermission(name, name, MultiTenancySides.Tenant);
    }

    @Test
    @DisplayName("合并宿主与租户角色：租户清单也写入 UrlPermission.tenants")
    void mergesHostAndTenantRoles() {
        RoleInfo hostRole = role("host-role");
        RoleInfo tenantRole = role("tenant-role");

        Map<String, List<RoleInfo>> allGrantPermission = new HashMap<>();
        allGrantPermission.put("p1", List.of(hostRole));
        allGrantPermission.put(1L + "p1", List.of(tenantRole));

        Map<String, List<Long>> allTenantPermission = new HashMap<>();
        allTenantPermission.put("p1", List.of(1L));

        UrlPermission target = permission("p1");
        Map<String, List<UrlPermission>> applicationPermission = Map.of("app", List.of(target));

        PermissionUtil.setPermissionRoleInfo(allGrantPermission, applicationPermission, allTenantPermission);

        assertThat(target.getTenants()).containsExactly(1L);
        assertThat(target.getRoles()).extracting(RoleInfo::getRoleCode)
                .containsExactly("host-role", "tenant-role");
    }

    @Test
    @DisplayName("租户清单为空时只合并宿主角色")
    void skipsTenantRolesWhenTenantsMissing() {
        Map<String, List<RoleInfo>> allGrantPermission = new HashMap<>();
        allGrantPermission.put("p1", List.of(role("host-role")));

        UrlPermission target = permission("p1");
        Map<String, List<UrlPermission>> applicationPermission = Map.of("app", List.of(target));

        PermissionUtil.setPermissionRoleInfo(allGrantPermission, applicationPermission, new HashMap<>());

        assertThat(target.getTenants()).isNull();
        assertThat(target.getRoles()).extracting(RoleInfo::getRoleCode).containsExactly("host-role");
    }

    @Test
    @DisplayName("宿主与租户都无授权时保持空的 roles 列表")
    void keepsEmptyRolesWhenNothingGranted() {
        Map<String, List<Long>> allTenantPermission = new HashMap<>();
        allTenantPermission.put("p1", List.of(1L, 2L));

        UrlPermission target = permission("p1");
        Map<String, List<UrlPermission>> applicationPermission = Map.of("app", List.of(target));

        PermissionUtil.setPermissionRoleInfo(new HashMap<>(), applicationPermission, allTenantPermission);

        assertThat(target.getTenants()).containsExactly(1L, 2L);
        assertThat(target.getRoles()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("租户未设置时即使存在租户授权记录也被忽略")
    void ignoresTenantGrantsWhenTenantNotEnabled() {
        Map<String, List<RoleInfo>> allGrantPermission = new HashMap<>();
        allGrantPermission.put(1L + "p1", List.of(role("tenant-role")));

        UrlPermission target = permission("p1");
        Map<String, List<UrlPermission>> applicationPermission = Map.of("app", List.of(target));

        PermissionUtil.setPermissionRoleInfo(allGrantPermission, applicationPermission, new HashMap<>());

        assertThat(target.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("多个应用、多个权限项都被合并")
    void mergesAcrossApplications() {
        Map<String, List<RoleInfo>> allGrantPermission = new HashMap<>();
        allGrantPermission.put("p1", List.of(role("r1")));
        allGrantPermission.put("p2", List.of(role("r2")));

        UrlPermission first = permission("p1");
        UrlPermission second = permission("p2");
        Map<String, List<UrlPermission>> applicationPermission = new HashMap<>();
        applicationPermission.put("app-a", List.of(first));
        applicationPermission.put("app-b", List.of(second));

        PermissionUtil.setPermissionRoleInfo(allGrantPermission, applicationPermission, new HashMap<>());

        assertThat(first.getRoles()).extracting(RoleInfo::getRoleCode).containsExactly("r1");
        assertThat(second.getRoles()).extracting(RoleInfo::getRoleCode).containsExactly("r2");
    }

    @Test
    @DisplayName("租户列表存在但对应租户授权缺失时只保留宿主角色")
    void tenantsWithoutGrantsKeepHostRolesOnly() {
        Map<String, List<RoleInfo>> allGrantPermission = new HashMap<>();
        allGrantPermission.put("p1", List.of(role("host-role")));

        Map<String, List<Long>> allTenantPermission = new HashMap<>();
        allTenantPermission.put("p1", List.of(1L));

        UrlPermission target = permission("p1");
        Map<String, List<UrlPermission>> applicationPermission = Map.of("app", List.of(target));

        PermissionUtil.setPermissionRoleInfo(allGrantPermission, applicationPermission, allTenantPermission);

        assertThat(target.getRoles()).extracting(RoleInfo::getRoleCode).containsExactly("host-role");
    }

    @Test
    @DisplayName("空的应用权限字典不产生任何副作用")
    void emptyApplicationPermissionsDoNothing() {
        Map<String, List<UrlPermission>> applicationPermission = new HashMap<>();
        UrlPermission target = permission("p1");
        applicationPermission.put("app", new ArrayList<>());

        PermissionUtil.setPermissionRoleInfo(new HashMap<>(), applicationPermission, new HashMap<>());

        assertThat(target.getRoles()).isEmpty();
        assertThat(target.getTenants()).isNull();
    }
}
