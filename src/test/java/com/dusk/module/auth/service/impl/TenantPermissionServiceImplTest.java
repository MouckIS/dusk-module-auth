package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.auth.dto.role.RolePermissionDto;
import com.dusk.module.auth.common.config.AppAuthConfig;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.dto.permission.EditionPermissionInputDto;
import com.dusk.module.auth.entity.SubscribableEdition;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.TenantPermission;
import com.dusk.module.auth.repository.ITenantPermissionRepository;
import com.dusk.module.auth.repository.ITenantRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TenantPermissionServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantPermissionServiceImplTest {

    @Mock
    private IAuthPermissionManager authPermissionManager;
    @Mock
    private JPAQueryFactory jpaQueryFactory;
    @Mock
    private ITenantRepository tenantRepository;
    @Mock
    private ITenantPermissionRepository repository;
    @Mock
    private AppAuthConfig appAuthConfig;

    private TenantPermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TenantPermissionServiceImpl();
        ReflectionTestUtils.setField(service, "authPermissionManager", authPermissionManager);
        ReflectionTestUtils.setField(service, "jpaQueryFactory", jpaQueryFactory);
        ReflectionTestUtils.setField(service, "tenantRepository", tenantRepository);
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "appAuthConfig", appAuthConfig);
    }

    private static Permission permission(String name) {
        return new Permission(name, name, MultiTenancySides.Host);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPAQuery stubSelectFrom(List<?> fetchResult) {
        JPAQuery query = mock(JPAQuery.class);
        when(jpaQueryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.fetch()).thenReturn((List) fetchResult);
        return query;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubDelete() {
        JPADeleteClause deleteClause = mock(JPADeleteClause.class);
        when(jpaQueryFactory.delete(any(EntityPath.class))).thenReturn(deleteClause);
        when(deleteClause.where(any(Predicate.class))).thenReturn(deleteClause);
        when(deleteClause.execute()).thenReturn(1L);
    }

    // ---------- getEditionPermissions ----------

    @Test
    @DisplayName("getEditionPermissions：editionId 为 null 时抛出业务异常")
    void getEditionPermissionsThrowsWhenEditionIdNull() {
        assertThatThrownBy(() -> service.getEditionPermissions(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getEditionPermissions：正常返回权限树并标记授权状态")
    void getEditionPermissionsReturnsTree() {
        TenantPermission granted = new TenantPermission();
        granted.setName("perm");
        stubSelectFrom(List.of(granted));
        when(authPermissionManager.getDefinitionPermissionTree(true))
                .thenReturn(List.of(permission("perm"), permission("other")));

        List<RolePermissionDto> result = service.getEditionPermissions(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isGranted()).isTrue();
        assertThat(result.get(1).isGranted()).isFalse();
    }

    // ---------- setEditionPermissions ----------

    @Test
    @DisplayName("setEditionPermissions：permissions 为 null 时不写入")
    void setEditionPermissionsSkipsWhenPermissionsNull() {
        stubDelete();
        EditionPermissionInputDto input = new EditionPermissionInputDto();
        input.setId(1L);

        service.setEditionPermissions(input);

        verify(repository, never()).saveAll(any());
        verify(authPermissionManager).refreshAll();
    }

    @Test
    @DisplayName("setEditionPermissions：permissions 为空集合时不写入")
    void setEditionPermissionsSkipsWhenPermissionsEmpty() {
        stubDelete();
        EditionPermissionInputDto input = new EditionPermissionInputDto();
        input.setId(1L);
        input.setPermissions(new ArrayList<>());

        service.setEditionPermissions(input);

        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("setEditionPermissions：permissions 非空时批量写入并刷新缓存")
    void setEditionPermissionsSavesWhenNotEmpty() {
        stubDelete();
        EditionPermissionInputDto input = new EditionPermissionInputDto();
        input.setId(1L);
        input.setPermissions(List.of("a", "b"));

        service.setEditionPermissions(input);

        verify(repository).saveAll(any());
        verify(authPermissionManager).refreshAll();
    }

    // ---------- getTenantPermissions ----------

    @Test
    @DisplayName("getTenantPermissions：禁用租户过滤时取全部定义权限")
    void getTenantPermissionsUsesDefinitionWhenFilterDisabled() {
        when(appAuthConfig.isDisableTenantAuthFilter()).thenReturn(true);
        when(authPermissionManager.getDefinitionPermission(true)).thenReturn(List.of("perm"));
        when(authPermissionManager.getDefinitionPermissionTree(true)).thenReturn(List.of(permission("perm")));

        List<RolePermissionDto> result = service.getTenantPermissions(1L);

        assertThat(result).hasSize(1);
        verify(tenantRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getTenantPermissions：启用租户过滤时按租户授权查询")
    void getTenantPermissionsUsesGrantedWhenFilterEnabled() {
        when(appAuthConfig.isDisableTenantAuthFilter()).thenReturn(false);
        Tenant tenant = new Tenant();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(authPermissionManager.getDefinitionPermissionTree(true)).thenReturn(List.of(permission("perm")));

        List<RolePermissionDto> result = service.getTenantPermissions(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isGranted()).isFalse();
    }

    // ---------- getGrantedPermissionByTenantId ----------

    @Test
    @DisplayName("getGrantedPermissionByTenantId：租户不存在时抛出业务异常")
    void getGrantedPermissionByTenantIdThrowsWhenTenantAbsent() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getGrantedPermissionByTenantId(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getGrantedPermissionByTenantId：租户存在且绑定版本时返回版本权限")
    void getGrantedPermissionByTenantIdWithEdition() {
        Tenant tenant = new Tenant();
        SubscribableEdition edition = new SubscribableEdition();
        edition.setId(9L);
        tenant.setEdition(edition);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        TenantPermission granted = new TenantPermission();
        granted.setName("perm");
        stubSelectFrom(List.of(granted));

        assertThat(service.getGrantedPermissionByTenantId(1L)).containsExactly("perm");
    }

    @Test
    @DisplayName("getGrantedPermissionByTenantId：租户未绑定版本时返回空集合")
    void getGrantedPermissionByTenantIdWithoutEdition() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(new Tenant()));

        assertThat(service.getGrantedPermissionByTenantId(1L)).isEmpty();
    }

    // ---------- getAllTenantPermission ----------

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    @DisplayName("getAllTenantPermission：禁用租户过滤时所有权限映射到全部租户")
    void getAllTenantPermissionWhenFilterDisabled() {
        when(appAuthConfig.isDisableTenantAuthFilter()).thenReturn(true);
        when(authPermissionManager.getDefinitionPermission(true)).thenReturn(List.of("perm"));

        JPAQuery query = mock(JPAQuery.class);
        when(jpaQueryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.from(any(EntityPath.class))).thenReturn(query);
        when(query.fetch()).thenReturn(List.of(1L, 2L));

        Map<String, List<Long>> result = service.getAllTenantPermission();

        assertThat(result).containsOnlyKeys("perm");
        assertThat(result.get("perm")).containsExactly(1L, 2L);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    @DisplayName("getAllTenantPermission：启用租户过滤时按版本权限分组映射租户")
    void getAllTenantPermissionWhenFilterEnabled() {
        when(appAuthConfig.isDisableTenantAuthFilter()).thenReturn(false);
        when(authPermissionManager.getDefinitionPermission(true)).thenReturn(List.of("perm", "other"));

        Tuple row = mock(Tuple.class);
        doReturn("perm").when(row).get(com.dusk.module.auth.entity.QTenantPermission.tenantPermission.name);
        doReturn(3L).when(row).get(com.dusk.module.auth.entity.QTenant.tenant.id);

        JPAQuery query = mock(JPAQuery.class);
        when(jpaQueryFactory.select(any(Expression.class), any(Expression.class))).thenReturn(query);
        when(query.from(any(EntityPath.class))).thenReturn(query);
        when(query.innerJoin(any(EntityPath.class))).thenReturn(query);
        when(query.on(any(Predicate.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.fetch()).thenReturn(List.of(row));

        Map<String, List<Long>> result = service.getAllTenantPermission();

        assertThat(result).containsOnlyKeys("perm", "other");
        assertThat(result.get("perm")).containsExactly(3L);
        assertThat(result.get("other")).isEmpty();
    }
}
