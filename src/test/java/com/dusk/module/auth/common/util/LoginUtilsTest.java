package com.dusk.module.auth.common.util;

import com.dusk.common.core.constant.AuthConstant;
import com.dusk.common.core.model.UserContext;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LoginUtils} 单元测试，目标：100% 分支覆盖。
 */
class LoginUtilsTest {

    private static final long TENANT_ID = 1000L;

    private static User user(boolean admin, Long tenantId, List<Role> roles) {
        User user = new User();
        user.setId(1L);
        user.setName("tester");
        user.setAdmin(admin);
        user.setTenantId(tenantId);
        user.setUserRoles(roles);
        return user;
    }

    private static Role role(long id) {
        Role role = new Role();
        role.setId(id);
        return role;
    }

    private static List<String> authorities(UserContext context) {
        return context.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("普通用户：只写入角色权限，不写入管理员权限")
    void nonAdminUserOnlyGetsRoleAuthorities() {
        UserContext context = LoginUtils.getUserContextByUser(
                user(false, TENANT_ID, List.of(role(11L), role(22L))));

        assertThat(context.getId()).isEqualTo(1L);
        assertThat(context.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(context.getName()).isEqualTo("tester");
        assertThat(context.getIsAdmin()).isFalse();
        assertThat(authorities(context)).containsExactly(
                AuthConstant.TYPE_ROLE + 11L,
                AuthConstant.TYPE_ROLE + 22L);
    }

    @Test
    @DisplayName("管理员 + 有租户：追加租户管理员权限")
    void adminUserWithTenantGetsTenantAdminAuthority() {
        UserContext context = LoginUtils.getUserContextByUser(
                user(true, TENANT_ID, Collections.emptyList()));

        assertThat(context.getIsAdmin()).isTrue();
        assertThat(authorities(context)).containsExactly(AuthConstant.ROLE_TENANT_ADMIN + TENANT_ID);
    }

    @Test
    @DisplayName("管理员 + 无租户：追加宿主管理员权限")
    void adminUserWithoutTenantGetsHostAdminAuthority() {
        UserContext context = LoginUtils.getUserContextByUser(
                user(true, null, Collections.emptyList()));

        assertThat(context.getIsAdmin()).isTrue();
        assertThat(authorities(context)).containsExactly(AuthConstant.ROLE_HOST_ADMIN);
    }
}
