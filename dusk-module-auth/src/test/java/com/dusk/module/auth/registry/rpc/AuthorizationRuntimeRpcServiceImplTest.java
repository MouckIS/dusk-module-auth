package com.dusk.module.auth.registry.rpc;

import com.dusk.common.core.auth.registry.AuthzScope;
import com.dusk.common.rpc.authz.ResourceRuntimeSnapshot;
import com.dusk.common.rpc.authz.RolePermissionSnapshot;
import com.dusk.common.rpc.authz.SubjectSnapshot;
import com.dusk.module.auth.authz.readmodel.AuthorizationReadModelResolver;
import com.dusk.module.auth.registry.service.ResourceRuntimeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link AuthorizationRuntimeRpcServiceImpl} 单元测试。
 *
 * <p>该 Provider 是网关 L1/L2 未命中时的回源入口，契约要求：查不到数据或内部异常都以
 * {@code null} 返回（而不是把异常透传给网关），避免一次查询失败把鉴权链路打成 500。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthorizationRuntimeRpcServiceImplTest {

    private static final String SERVICE_ID = "dusk-module-user";
    private static final String TENANT_SCOPE = "TENANT:1";

    @Mock
    private ResourceRuntimeResolver resourceRuntimeResolver;
    @Mock
    private AuthorizationReadModelResolver authorizationReadModelResolver;

    private AuthorizationRuntimeRpcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthorizationRuntimeRpcServiceImpl(resourceRuntimeResolver, authorizationReadModelResolver);
    }

    @Test
    @DisplayName("getResourceSnapshot：有有效资源并集时原样返回")
    void returnsResourceSnapshot() {
        ResourceRuntimeSnapshot snapshot = new ResourceRuntimeSnapshot(SERVICE_ID, "rv1", "1.0.0", 3L,
                List.of(new ResourceRuntimeSnapshot.RuntimeRoute("GET", "/users", "p1")), List.of());
        when(resourceRuntimeResolver.resolve(SERVICE_ID)).thenReturn(Optional.of(snapshot));

        assertThat(service.getResourceSnapshot(SERVICE_ID)).isSameAs(snapshot);
    }

    @Test
    @DisplayName("getResourceSnapshot：服务未登记或无可承接流量的 Release 时返回 null")
    void returnsNullWhenResourceSnapshotMissing() {
        when(resourceRuntimeResolver.resolve(SERVICE_ID)).thenReturn(Optional.empty());

        assertThat(service.getResourceSnapshot(SERVICE_ID)).isNull();
    }

    @Test
    @DisplayName("getRolePermissions：命中作用域时返回权限集合")
    void returnsRolePermissions() {
        RolePermissionSnapshot snapshot = new RolePermissionSnapshot(TENANT_SCOPE, 5L, 7L, Set.of("p1"));
        when(authorizationReadModelResolver.resolveRolePermissions(TENANT_SCOPE, 5L))
                .thenReturn(Optional.of(snapshot));

        assertThat(service.getRolePermissions(TENANT_SCOPE, 5L)).isSameAs(snapshot);
    }

    @Test
    @DisplayName("getRolePermissions：角色不存在时返回 null")
    void returnsNullWhenRoleMissing() {
        when(authorizationReadModelResolver.resolveRolePermissions(TENANT_SCOPE, 5L))
                .thenReturn(Optional.empty());

        assertThat(service.getRolePermissions(TENANT_SCOPE, 5L)).isNull();
    }

    @Test
    @DisplayName("getRolePermissions：作用域非法等异常被吞掉并返回 null（不透传给网关）")
    void swallowsRoleLookupFailure() {
        assertThat(service.getRolePermissions("bad-scope", 5L)).isNull();
        assertThat(service.getRolePermissions(null, 5L)).isNull();

        when(authorizationReadModelResolver.resolveRolePermissions(AuthzScope.HOST, 5L))
                .thenThrow(new IllegalStateException("db down"));
        assertThat(service.getRolePermissions(AuthzScope.HOST, 5L)).isNull();
    }

    @Test
    @DisplayName("getSubject：命中作用域时返回账号状态快照")
    void returnsSubject() {
        SubjectSnapshot snapshot = new SubjectSnapshot(TENANT_SCOPE, 9L, 2L, true, Set.of(3L));
        when(authorizationReadModelResolver.resolveSubject(TENANT_SCOPE, 9L)).thenReturn(Optional.of(snapshot));

        assertThat(service.getSubject(TENANT_SCOPE, 9L)).isSameAs(snapshot);
    }

    @Test
    @DisplayName("getSubject：用户不存在时返回 null")
    void returnsNullWhenSubjectMissing() {
        when(authorizationReadModelResolver.resolveSubject(TENANT_SCOPE, 9L)).thenReturn(Optional.empty());

        assertThat(service.getSubject(TENANT_SCOPE, 9L)).isNull();
    }

    @Test
    @DisplayName("getSubject：作用域非法等异常被吞掉并返回 null")
    void swallowsSubjectLookupFailure() {
        assertThat(service.getSubject("TENANT:", 9L)).isNull();

        when(authorizationReadModelResolver.resolveSubject(AuthzScope.HOST, 9L))
                .thenThrow(new IllegalStateException("db down"));
        assertThat(service.getSubject(AuthzScope.HOST, 9L)).isNull();
    }
}
