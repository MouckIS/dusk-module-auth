package com.dusk.module.auth.manage;

import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ITenantPermissionService;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link UserManage} 单元测试：覆盖用户/角色查询、当前登录人、宿主与租户两种权限来源、
 * 管理员短路、角色授权过滤以及按角色名查用户。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserManageTest {

    @Mock
    private IUserRepository userRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private IAuthPermissionManager authPermissionManager;
    @Mock
    private IGrantPermissionRepository grantPermissionRepository;
    @Mock
    private ITenantPermissionService tenantPermissionService;
    @Mock
    private JPAQueryFactory jpaQueryFactory;
    @Mock(answer = Answers.RETURNS_SELF)
    private JPAQuery<Long> outerQuery;
    @Mock(answer = Answers.RETURNS_SELF)
    private JPAQuery<Long> innerQuery;

    private UserManage userManage;

    private static User user(Long id, Long tenantId, boolean admin) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setAdmin(admin);
        return user;
    }

    private static Role role(Long id) {
        Role role = new Role();
        role.setId(id);
        return role;
    }

    private static GrantPermission grantPermission(String name) {
        GrantPermission grantPermission = new GrantPermission();
        grantPermission.setName(name);
        return grantPermission;
    }

    @BeforeEach
    void setUp() {
        userManage = new UserManage();
        ReflectionTestUtils.setField(userManage, "userRepository", userRepository);
        ReflectionTestUtils.setField(userManage, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(userManage, "authPermissionManager", authPermissionManager);
        ReflectionTestUtils.setField(userManage, "grantPermissionRepository", grantPermissionRepository);
        ReflectionTestUtils.setField(userManage, "tenantPermissionService", tenantPermissionService);
        ReflectionTestUtils.setField(userManage, "jpaQueryFactory", jpaQueryFactory);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("getUserRole：用户存在时返回角色列表")
    void getUserRoleFound() {
        User user = user(1L, 10L, false);
        user.setUserRoles(List.of(role(2L)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userManage.getUserRole(1L)).extracting(Role::getId).containsExactly(2L);
    }

    @Test
    @DisplayName("getUserRole：用户不存在时返回空列表")
    void getUserRoleMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(userManage.getUserRole(1L)).isEmpty();
    }

    @Test
    @DisplayName("getCurrentUser：无登录上下文返回 null")
    void getCurrentUserWithoutContext() {
        when(securityUtils.getCurrentUser()).thenReturn(null);

        assertThat(userManage.getCurrentUser()).isNull();
    }

    @Test
    @DisplayName("getCurrentUser：上下文无用户 id 返回 null")
    void getCurrentUserWithoutId() {
        when(securityUtils.getCurrentUser()).thenReturn(new UserContext());

        assertThat(userManage.getCurrentUser()).isNull();
    }

    @Test
    @DisplayName("getCurrentUser：按上下文 id 查询用户")
    void getCurrentUserFound() {
        UserContext userContext = new UserContext();
        userContext.setId(1L);
        User user = user(1L, 10L, false);
        when(securityUtils.getCurrentUser()).thenReturn(userContext);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userManage.getCurrentUser()).isSameAs(user);
    }

    @Test
    @DisplayName("getUserInfo：存在返回用户，不存在返回 null")
    void getUserInfo() {
        User user = user(1L, 10L, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(userManage.getUserInfo(1L)).isSameAs(user);
        assertThat(userManage.getUserInfo(2L)).isNull();
    }

    @Test
    @DisplayName("getCurrentUserPermissions：使用当前登录人 id 计算权限")
    void getCurrentUserPermissions() {
        UserContext userContext = new UserContext();
        userContext.setId(1L);
        when(securityUtils.getCurrentUser()).thenReturn(userContext);
        User user = user(1L, null, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of("p1"));

        assertThat(userManage.getCurrentUserPermissions()).containsExactly("p1");
    }

    @Test
    @DisplayName("宿主管理员：未选择租户时返回 Host 侧全部权限")
    void hostAdminGetsHostPermissions() {
        User user = user(1L, null, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of("p1", "p2"));

        assertThat(userManage.getUserPermissions(1L)).containsExactly("p1", "p2");
        verify(authPermissionManager).getDefinitionPermission(false);
    }

    @Test
    @DisplayName("宿主管理员：选择租户后按 Tenant 侧过滤权限")
    void hostAdminWithTenantContextGetsTenantPermissions() {
        TenantContextHolder.setTenantId(9L);
        User user = user(1L, null, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authPermissionManager.getDefinitionPermission(true)).thenReturn(List.of("t1"));

        assertThat(userManage.getUserPermissions(1L)).containsExactly("t1");
        verify(authPermissionManager).getDefinitionPermission(true);
    }

    @Test
    @DisplayName("租户管理员：直接返回租户已授权权限")
    void tenantAdminGetsTenantPermissions() {
        User user = user(1L, 5L, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tenantPermissionService.getGrantedPermissionByTenantId(5L)).thenReturn(List.of("t1", "t2"));

        assertThat(userManage.getUserPermissions(1L)).containsExactly("t1", "t2");
    }

    @Test
    @DisplayName("宿主普通用户：角色授权与宿主权限取交集")
    void hostUserPermissionsAreIntersectedWithRoleGrants() {
        User user = user(1L, null, false);
        user.setUserRoles(List.of(role(2L), role(3L)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of("p1", "p2"));
        when(grantPermissionRepository.findDistinctByRoleIdIn(any(Long[].class)))
                .thenReturn(List.of(grantPermission("p1"), grantPermission("p3")));

        assertThat(userManage.getUserPermissions(1L)).containsExactly("p1");
    }

    @Test
    @DisplayName("宿主普通用户：没有角色时返回空列表")
    void userWithoutRolesGetsEmptyPermissions() {
        User user = user(1L, null, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of("p1"));

        assertThat(userManage.getUserPermissions(1L)).isEmpty();
        verifyNoInteractions(grantPermissionRepository);
    }

    @Test
    @DisplayName("租户普通用户：角色授权与租户权限取交集")
    void tenantUserPermissionsAreIntersected() {
        User user = user(1L, 5L, false);
        user.setUserRoles(List.of(role(2L)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tenantPermissionService.getGrantedPermissionByTenantId(5L)).thenReturn(List.of("t1"));
        when(grantPermissionRepository.findDistinctByRoleIdIn(any(Long[].class)))
                .thenReturn(List.of(grantPermission("t1"), grantPermission("t2")));

        assertThat(userManage.getUserPermissions(1L)).containsExactly("t1");
    }

    @Test
    @DisplayName("getUserIdsByRoleName：角色名为空返回空列表")
    void getUserIdsByRoleNameWithoutNames() {
        assertThat(userManage.getUserIdsByRoleName(null)).isEmpty();
        verifyNoInteractions(jpaQueryFactory);
    }

    @Test
    @DisplayName("getUserIdsByRoleName：构造 QueryDSL 查询并返回用户 id")
    void getUserIdsByRoleName() {
        doReturn(outerQuery).when(jpaQueryFactory).selectDistinct(any(Expression.class));
        doReturn(innerQuery).when(jpaQueryFactory).select(any(Expression.class));
        doReturn(List.of(7L)).when(outerQuery).fetch();

        assertThat(userManage.getUserIdsByRoleName(List.of("admin"))).containsExactly(7L);
    }
}
