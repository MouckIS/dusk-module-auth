package com.dusk.module.auth.authz.readmodel;

import com.dusk.common.core.enums.UserStatus;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.authz.RolePermissionSnapshot;
import com.dusk.common.rpc.authz.SubjectSnapshot;
import com.dusk.module.auth.authz.version.AuthzRoleVersion;
import com.dusk.module.auth.authz.version.AuthzSubjectVersion;
import com.dusk.module.auth.authz.version.IAuthzRoleVersionRepository;
import com.dusk.module.auth.authz.version.IAuthzSubjectVersionRepository;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.registry.entity.PermissionDefinition;
import com.dusk.module.auth.registry.enums.PermissionStatus;
import com.dusk.module.auth.registry.repository.IPermissionDefinitionRepository;
import com.dusk.module.auth.repository.IRoleRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ITenantPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * {@link AuthorizationReadModelResolver} 单元测试：回源解析必须与网关读模型看到同一份事实。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthorizationReadModelResolverTest {

    private static final long ROLE_ID = 9L;
    private static final Long USER_ID = 7L;

    @Mock
    private IRoleRepository roleRepository;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private IPermissionDefinitionRepository permissionRepository;
    @Mock
    private ITenantPermissionService tenantPermissionService;
    @Mock
    private IAuthzRoleVersionRepository roleVersionRepository;
    @Mock
    private IAuthzSubjectVersionRepository subjectVersionRepository;

    private AuthorizationReadModelResolver resolver;

    private static Role role(Long tenantId, String... permissionNames) {
        Role role = new Role();
        role.setTenantId(tenantId);
        List<GrantPermission> permissions = new ArrayList<>();
        for (String name : permissionNames) {
            GrantPermission permission = new GrantPermission();
            permission.setName(name);
            permissions.add(permission);
        }
        role.setPermissions(permissions);
        return role;
    }

    private static User user(Long tenantId, boolean active, UserStatus status, Role... roles) {
        User user = new User();
        user.setTenantId(tenantId);
        user.setActive(active);
        user.setUserStatus(status);
        user.setUserRoles(new ArrayList<>(List.of(roles)));
        return user;
    }

    private static AuthzRoleVersion roleVersion(long version) {
        AuthzRoleVersion row = new AuthzRoleVersion();
        row.setPermissionVersion(version);
        return row;
    }

    private static AuthzSubjectVersion subjectVersion(long version) {
        AuthzSubjectVersion row = new AuthzSubjectVersion();
        row.setSubjectVersion(version);
        return row;
    }

    private static PermissionDefinition definition(String code, PermissionStatus status) {
        PermissionDefinition definition = new PermissionDefinition();
        definition.setCode(code);
        definition.setStatus(status);
        return definition;
    }

    @BeforeEach
    void setUp() {
        resolver = new AuthorizationReadModelResolver(roleRepository, userRepository, permissionRepository,
                tenantPermissionService, roleVersionRepository, subjectVersionRepository);
    }

    @Test
    @DisplayName("enabled：未激活、离职均不可用；状态为空按在职兼容")
    void shouldComputeEnabled() {
        User inactive = user(null, false, UserStatus.OnJob);
        User onJob = user(null, true, UserStatus.OnJob);
        User dimission = user(null, true, UserStatus.Dimission);
        User nullStatus = user(null, true, null);

        assertThat(AuthorizationReadModelResolver.enabled(inactive)).isFalse();
        assertThat(AuthorizationReadModelResolver.enabled(onJob)).isTrue();
        assertThat(AuthorizationReadModelResolver.enabled(dimission)).isFalse();
        assertThat(AuthorizationReadModelResolver.enabled(nullStatus)).isTrue();
    }

    @Test
    @DisplayName("空/非法作用域：回源前直接拒绝")
    void shouldRejectInvalidScope() {
        assertThatThrownBy(() -> resolver.resolveRolePermissions(null, ROLE_ID))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> resolver.resolveRolePermissions("  ", ROLE_ID))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> resolver.resolveRolePermissions("bad", ROLE_ID))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> resolver.resolveSubject("bad", USER_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("roleId 为空：返回空，不查询仓储")
    void shouldReturnEmptyForNullRoleId() {
        assertThat(resolver.resolveRolePermissions("HOST", null)).isEmpty();
        verify(roleRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("角色不存在但版本记录存在：返回空权限快照以覆盖旧缓存")
    void shouldReturnEmptySnapshotForDeletedRole() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());
        when(roleVersionRepository.findByTenantScopeAndRoleId("HOST", ROLE_ID))
                .thenReturn(Optional.of(roleVersion(6L)));

        Optional<RolePermissionSnapshot> snapshot = resolver.resolveRolePermissions("HOST", ROLE_ID);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().tenantScope()).isEqualTo("HOST");
        assertThat(snapshot.get().roleId()).isEqualTo(ROLE_ID);
        assertThat(snapshot.get().permissionVersion()).isEqualTo(6L);
        assertThat(snapshot.get().permissions()).isEmpty();
    }

    @Test
    @DisplayName("角色与版本记录都不存在：返回空")
    void shouldReturnEmptyForUnknownRole() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());
        when(roleVersionRepository.findByTenantScopeAndRoleId("HOST", ROLE_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolveRolePermissions("HOST", ROLE_ID)).isEmpty();
    }

    @Test
    @DisplayName("用户不存在但代际存在：返回 enabled=false 的快照，显式失效旧令牌")
    void shouldReturnDisabledSnapshotForDeletedUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(subjectVersionRepository.findByTenantScopeAndUserId("TENANT:5", USER_ID))
                .thenReturn(Optional.of(subjectVersion(3L)));

        Optional<SubjectSnapshot> snapshot = resolver.resolveSubject("TENANT:5", USER_ID);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().enabled()).isFalse();
        assertThat(snapshot.get().subjectVersion()).isEqualTo(3L);
        assertThat(snapshot.get().roleIds()).isEmpty();
    }

    @Test
    @DisplayName("用户与代际都不存在：返回空")
    void shouldReturnEmptyForUnknownUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(subjectVersionRepository.findByTenantScopeAndUserId("TENANT:5", USER_ID))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolveSubject("TENANT:5", USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("Host 角色：不经过租户上界，版本缺失按 0")
    void shouldResolveHostRoleWithoutTenantBound() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(null, "a", "b")));
        when(roleVersionRepository.findByTenantScopeAndRoleId("HOST", ROLE_ID)).thenReturn(Optional.empty());

        RolePermissionSnapshot snapshot = resolver.resolveRolePermissions("HOST", ROLE_ID).orElseThrow();

        assertThat(snapshot.permissionVersion()).isZero();
        assertThat(snapshot.permissions()).containsExactlyInAnyOrder("a", "b");
        verify(tenantPermissionService, never()).getGrantedPermissionByTenantId(any());
    }

    @Test
    @DisplayName("Host 作用域遇到带租户的角色：作用域不匹配，拒绝回源")
    void shouldRejectHostScopeForTenantRole() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(5L, "a")));

        assertThat(resolver.resolveRolePermissions("HOST", ROLE_ID)).isEmpty();
    }

    @Test
    @DisplayName("租户作用域遇到 Host 角色：作用域不匹配，拒绝回源")
    void shouldRejectTenantScopeForHostRole() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(null, "a")));

        assertThat(resolver.resolveRolePermissions("TENANT:5", ROLE_ID)).isEmpty();
    }

    @Test
    @DisplayName("租户作用域遇到其它租户角色：拒绝回源")
    void shouldRejectCrossTenantRole() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(6L, "a")));

        assertThat(resolver.resolveRolePermissions("TENANT:5", ROLE_ID)).isEmpty();
    }

    @Test
    @DisplayName("租户作用域：角色权限与租户授权取交集，版本来自版本表")
    void shouldIntersectWithTenantPermissions() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(5L, "a", "b")));
        when(tenantPermissionService.getGrantedPermissionByTenantId(5L)).thenReturn(List.of("b", "c"));
        when(roleVersionRepository.findByTenantScopeAndRoleId("TENANT:5", ROLE_ID))
                .thenReturn(Optional.of(roleVersion(4L)));

        RolePermissionSnapshot snapshot = resolver.resolveRolePermissions("TENANT:5", ROLE_ID).orElseThrow();

        assertThat(snapshot.permissions()).containsExactly("b");
        assertThat(snapshot.permissionVersion()).isEqualTo(4L);
    }

    @Test
    @DisplayName("租户权限解析异常：按默认拒绝返回空集合")
    void shouldDenyAllWhenTenantPermissionFails() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(5L, "a")));
        when(tenantPermissionService.getGrantedPermissionByTenantId(5L))
                .thenThrow(new IllegalStateException("edition missing"));

        RolePermissionSnapshot snapshot = resolver.resolveRolePermissions("TENANT:5", ROLE_ID).orElseThrow();

        assertThat(snapshot.permissions()).isEmpty();
    }

    @Test
    @DisplayName("运行时权限过滤：ORPHANED/DISABLED 剔除，未登记权限保留（迁移期兼容）")
    void shouldFilterNonActivePermissions() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(null, "a", "b", "c", "d")));
        when(permissionRepository.findByCodeIn(any()))
                .thenReturn(List.of(
                        definition("a", PermissionStatus.ACTIVE),
                        definition("a", PermissionStatus.ORPHANED),
                        definition("b", PermissionStatus.ORPHANED),
                        definition("c", PermissionStatus.DISABLED)));

        RolePermissionSnapshot snapshot = resolver.resolveRolePermissions("HOST", ROLE_ID).orElseThrow();

        assertThat(snapshot.permissions()).containsExactlyInAnyOrder("a", "d");
    }

    @Test
    @DisplayName("权限名为空/空白：收集阶段剔除")
    void shouldDropBlankPermissionNames() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(null, "a", null, "  ", "")));

        RolePermissionSnapshot snapshot = resolver.resolveRolePermissions("HOST", ROLE_ID).orElseThrow();

        assertThat(snapshot.permissions()).containsExactly("a");
    }

    @Test
    @DisplayName("角色权限集合为 null：按空集合处理")
    void shouldTreatNullPermissionsAsEmpty() {
        Role role = role(null);
        role.setPermissions(null);
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));

        RolePermissionSnapshot snapshot = resolver.resolveRolePermissions("HOST", ROLE_ID).orElseThrow();

        assertThat(snapshot.permissions()).isEmpty();
    }

    @Test
    @DisplayName("Host 用户：返回代际、角色 ID 与在职状态")
    void shouldResolveHostSubject() {
        Role role = new Role();
        role.setId(3L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(null, true, UserStatus.OnJob, role)));
        when(subjectVersionRepository.findByTenantScopeAndUserId("HOST", USER_ID)).thenReturn(Optional.empty());

        SubjectSnapshot snapshot = resolver.resolveSubject("HOST", USER_ID).orElseThrow();

        assertThat(snapshot.enabled()).isTrue();
        assertThat(snapshot.subjectVersion()).isZero();
        assertThat(snapshot.roleIds()).containsExactly(3L);
    }

    @Test
    @DisplayName("用户归属其它租户：拒绝回源")
    void shouldRejectCrossTenantSubject() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(6L, true, UserStatus.OnJob)));

        assertThat(resolver.resolveSubject("TENANT:5", USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("Host 作用域遇到带租户的用户：拒绝回源")
    void shouldRejectHostScopeForTenantUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(5L, true, UserStatus.OnJob)));

        assertThat(resolver.resolveSubject("HOST", USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("用户角色集合为 null：角色 ID 为空集合")
    void shouldTreatNullRolesAsEmpty() {
        User user = user(5L, true, UserStatus.OnJob);
        user.setUserRoles(null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(subjectVersionRepository.findByTenantScopeAndUserId("TENANT:5", USER_ID))
                .thenReturn(Optional.of(subjectVersion(2L)));

        SubjectSnapshot snapshot = resolver.resolveSubject("TENANT:5", USER_ID).orElseThrow();

        assertThat(snapshot.roleIds()).isEmpty();
        assertThat(snapshot.subjectVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("账号状态：未激活/离职 enabled=false，在职或状态为空 enabled=true")
    void shouldResolveSubjectEnabledFlag() {
        when(subjectVersionRepository.findByTenantScopeAndUserId(any(), anyLong())).thenReturn(Optional.empty());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user(5L, false, UserStatus.OnJob)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(5L, true, UserStatus.Dimission)));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user(5L, true, null)));
        when(userRepository.findById(4L)).thenReturn(Optional.of(user(5L, true, UserStatus.OnJob)));

        assertThat(resolver.resolveSubject("TENANT:5", 1L).orElseThrow().enabled()).isFalse();
        assertThat(resolver.resolveSubject("TENANT:5", 2L).orElseThrow().enabled()).isFalse();
        assertThat(resolver.resolveSubject("TENANT:5", 3L).orElseThrow().enabled()).isTrue();
        assertThat(resolver.resolveSubject("TENANT:5", 4L).orElseThrow().enabled()).isTrue();
    }

    @Test
    @DisplayName("userId 为空：返回空，不查询仓储")
    void shouldReturnEmptyForNullUserId() {
        assertThat(resolver.resolveSubject("HOST", null)).isEmpty();
        verify(userRepository, never()).findById(any());
    }
}
