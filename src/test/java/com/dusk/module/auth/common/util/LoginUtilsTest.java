package com.dusk.module.auth.common.util;

import com.dusk.common.core.constant.AuthConstant;
import com.dusk.common.core.model.UserContext;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.entity.UserRole;
import com.dusk.module.auth.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 登录工具类单元测试 - 100%分支覆盖
 *
 * @author kefuming
 * @date 2026-02-28
 */
@DisplayName("登录工具类测试")
class LoginUtilsTest {

    @DisplayName("getUserContextByUser - 普通用户（非管理员）")
    @Test
    void testGetUserContextByUser_NormalUser() {
        // Arrange
        User user = TestDataBuilder.buildUser(1L, "testUser", "user@example.com", false);
        Role role = TestDataBuilder.buildRole(1L, "USER", "User Role");
        UserRole userRole = TestDataBuilder.buildUserRole(user, role);
        user.getUserRoles().add(userRole);

        // Act
        UserContext context = LoginUtils.getUserContextByUser(user);

        // Assert
        assertThat(context).isNotNull();
        assertThat(context.getId()).isEqualTo(1L);
        assertThat(context.getName()).isEqualTo("testUser");
        assertThat(context.getTenantId()).isEqualTo(1L);
        assertThat(context.isAdmin()).isFalse();
        assertThat(context.getAuthorities()).hasSize(1);
        assertThat(context.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(AuthConstant.TYPE_ROLE + 1L)))
                .isTrue();
    }

    @DisplayName("getUserContextByUser - 租户管理员")
    @Test
    void testGetUserContextByUser_TenantAdmin() {
        // Arrange
        User user = TestDataBuilder.buildUser(2L, "adminUser", "admin@example.com", true);
        user.setTenantId(1L);
        Role role = TestDataBuilder.buildRole(1L, "ADMIN", "Admin Role");
        UserRole userRole = TestDataBuilder.buildUserRole(user, role);
        user.getUserRoles().add(userRole);

        // Act
        UserContext context = LoginUtils.getUserContextByUser(user);

        // Assert
        assertThat(context).isNotNull();
        assertThat(context.getId()).isEqualTo(2L);
        assertThat(context.isAdmin()).isTrue();
        assertThat(context.getTenantId()).isEqualTo(1L);
        // 应该有角色权限和租户管理员权限
        assertThat(context.getAuthorities()).hasSize(2);
        assertThat(context.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(AuthConstant.ROLE_TENANT_ADMIN + 1L)))
                .isTrue();
        assertThat(context.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(AuthConstant.TYPE_ROLE + 1L)))
                .isTrue();
    }

    @DisplayName("getUserContextByUser - 主机管理员（无租户）")
    @Test
    void testGetUserContextByUser_HostAdmin() {
        // Arrange
        User user = TestDataBuilder.buildUser(3L, "hostAdmin", "host-admin@example.com", true);
        user.setTenantId(null);
        Role role = TestDataBuilder.buildRole(1L, "SUPER_ADMIN", "Super Admin Role");
        UserRole userRole = TestDataBuilder.buildUserRole(user, role);
        user.getUserRoles().add(userRole);

        // Act
        UserContext context = LoginUtils.getUserContextByUser(user);

        // Assert
        assertThat(context).isNotNull();
        assertThat(context.getId()).isEqualTo(3L);
        assertThat(context.isAdmin()).isTrue();
        assertThat(context.getTenantId()).isNull();
        // 应该有角色权限和主机管理员权限
        assertThat(context.getAuthorities()).hasSize(2);
        assertThat(context.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(AuthConstant.ROLE_HOST_ADMIN)))
                .isTrue();
        assertThat(context.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(AuthConstant.TYPE_ROLE + 1L)))
                .isTrue();
    }

    @DisplayName("getUserContextByUser - 用户有多个角色")
    @Test
    void testGetUserContextByUser_MultipleRoles() {
        // Arrange
        User user = TestDataBuilder.buildUser(4L, "multiRoleUser", "multi@example.com", false);
        Role role1 = TestDataBuilder.buildRole(1L, "ROLE_USER", "User Role");
        Role role2 = TestDataBuilder.buildRole(2L, "ROLE_EDITOR", "Editor Role");
        Role role3 = TestDataBuilder.buildRole(3L, "ROLE_REVIEWER", "Reviewer Role");

        user.getUserRoles().add(TestDataBuilder.buildUserRole(user, role1));
        user.getUserRoles().add(TestDataBuilder.buildUserRole(user, role2));
        user.getUserRoles().add(TestDataBuilder.buildUserRole(user, role3));

        // Act
        UserContext context = LoginUtils.getUserContextByUser(user);

        // Assert
        assertThat(context).isNotNull();
        assertThat(context.getAuthorities()).hasSize(3);
        assertThat(context.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
                .contains(
                        AuthConstant.TYPE_ROLE + 1L,
                        AuthConstant.TYPE_ROLE + 2L,
                        AuthConstant.TYPE_ROLE + 3L
                );
    }

    @DisplayName("getUserContextByUser - 用户没有角色")
    @Test
    void testGetUserContextByUser_NoRoles() {
        // Arrange
        User user = TestDataBuilder.buildUser(5L, "noRoleUser", "norole@example.com", false);
        // 不添加任何角色

        // Act
        UserContext context = LoginUtils.getUserContextByUser(user);

        // Assert
        assertThat(context).isNotNull();
        assertThat(context.getId()).isEqualTo(5L);
        assertThat(context.isAdmin()).isFalse();
        assertThat(context.getAuthorities()).isEmpty();
    }

    @DisplayName("getUserContextByUser - 租户管理员但无租户ID")
    @Test
    void testGetUserContextByUser_AdminNoTenantId() {
        // Arrange
        User user = TestDataBuilder.buildUser(6L, "adminNoTenant", "admin-no-tenant@example.com", true);
        user.setTenantId(null);
        Role role = TestDataBuilder.buildRole(2L, "ADMIN_ROLE", "Admin Role");
        user.getUserRoles().add(TestDataBuilder.buildUserRole(user, role));

        // Act
        UserContext context = LoginUtils.getUserContextByUser(user);

        // Assert
        assertThat(context).isNotNull();
        assertThat(context.isAdmin()).isTrue();
        assertThat(context.getTenantId()).isNull();
        // 应该有角色权限和HOST_ADMIN权限（因为无租户）
        assertThat(context.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(AuthConstant.ROLE_HOST_ADMIN)))
                .isTrue();
    }

    @DisplayName("getUserContextByUser - 用户信息完整性")
    @Test
    void testGetUserContextByUser_DataIntegrity() {
        // Arrange
        User user = TestDataBuilder.buildUser(7L, "fullUser", "full@example.com", false);
        user.setTenantId(99L);
        Role role = TestDataBuilder.buildRole(10L, "ROLE_TEST", "Test Role");
        user.getUserRoles().add(TestDataBuilder.buildUserRole(user, role));

        // Act
        UserContext context = LoginUtils.getUserContextByUser(user);

        // Assert
        assertThat(context).isNotNull();
        assertThat(context.getId()).isEqualTo(7L);
        assertThat(context.getName()).isEqualTo("fullUser");
        assertThat(context.getTenantId()).isEqualTo(99L);
        assertThat(context.isAdmin()).isFalse();
        assertThat(context.getAuthorities()).isNotEmpty();

        // 验证authority格式
        List<String> authorityStrings = context.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertThat(authorityStrings).contains(AuthConstant.TYPE_ROLE + 10L);
    }

    @DisplayName("getUserContextByUser - 特殊字符用户名")
    @Test
    void testGetUserContextByUser_SpecialCharacterUsername() {
        // Arrange
        User user = TestDataBuilder.buildUser(8L, "user@特殊字符", "special@example.com", false);

        // Act
        UserContext context = LoginUtils.getUserContextByUser(user);

        // Assert
        assertThat(context).isNotNull();
        assertThat(context.getName()).isEqualTo("user@特殊字符");
    }

    @DisplayName("getUserContextByUser - 空字符串用户名")
    @Test
    void testGetUserContextByUser_EmptyUsername() {
        // Arrange
        User user = TestDataBuilder.buildUser(9L, "", "empty@example.com", false);

        // Act
        UserContext context = LoginUtils.getUserContextByUser(user);

        // Assert
        assertThat(context).isNotNull();
        assertThat(context.getName()).isEmpty();
    }

    @DisplayName("getUserContextByUser - 长用户名")
    @Test
    void testGetUserContextByUser_LongUsername() {
        // Arrange
        String longName = "a".repeat(500);
        User user = TestDataBuilder.buildUser(10L, longName, "long@example.com", false);

        // Act
        UserContext context = LoginUtils.getUserContextByUser(user);

        // Assert
        assertThat(context).isNotNull();
        assertThat(context.getName()).isEqualTo(longName);
        assertThat(context.getName()).hasSize(500);
    }
}

