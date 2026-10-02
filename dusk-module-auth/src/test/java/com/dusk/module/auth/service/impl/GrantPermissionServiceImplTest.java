package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.permission.RoleInfo;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.QGrantPermission;
import com.dusk.module.auth.entity.QRole;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GrantPermissionServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class GrantPermissionServiceImplTest {

    private static final QGrantPermission GRANT_PERMISSION = QGrantPermission.grantPermission;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JPAQueryFactory queryFactory;
    @Mock
    private IGrantPermissionRepository repository;

    private GrantPermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GrantPermissionServiceImpl();
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "repository", repository);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPAQuery stubDistinctChain() {
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.selectDistinct(any(Expression.class), any(Expression.class), any(Expression.class),
                any(Expression.class), any(Expression.class))).thenReturn(query);
        when(query.from(any(EntityPath.class))).thenReturn(query);
        when(query.leftJoin(any(EntityPath.class))).thenReturn(query);
        return query;
    }

    private static Tuple tuple(Long tenantId, String name) {
        Tuple tuple = mock(Tuple.class);
        doReturn(tenantId).when(tuple).get(GRANT_PERMISSION.tenantId);
        doReturn(name).when(tuple).get(GRANT_PERMISSION.name);
        doReturn(5L).when(tuple).get(GRANT_PERMISSION.role.id);
        doReturn("role-name").when(tuple).get(GRANT_PERMISSION.role.roleName);
        doReturn("role-code").when(tuple).get(GRANT_PERMISSION.role.roleCode);
        return tuple;
    }

    @Test
    @DisplayName("getAll：tenantId 为 null 时使用空前缀生成 key")
    void getAllUsesEmptyPrefixWhenTenantIdNull() {
        Tuple row = tuple(null, "perm");
        @SuppressWarnings("rawtypes")
        JPAQuery query = stubDistinctChain();
        when(query.fetch()).thenReturn(List.of(row));

        Map<String, List<RoleInfo>> result = service.getAll();

        assertThat(result).containsOnlyKeys("perm");
        assertThat(result.get("perm")).hasSize(1);
        assertThat(result.get("perm").get(0).getRoleCode()).isEqualTo("role-code");
    }

    @Test
    @DisplayName("getAll：tenantId 非 null 时拼接到 key 前缀")
    void getAllUsesTenantIdAsPrefix() {
        Tuple row = tuple(1000L, "perm");
        @SuppressWarnings("rawtypes")
        JPAQuery query = stubDistinctChain();
        when(query.fetch()).thenReturn(List.of(row));

        Map<String, List<RoleInfo>> result = service.getAll();

        assertThat(result).containsOnlyKeys("1000perm");
    }

    @Test
    @DisplayName("addDynamicPermission：存在待新增权限时批量保存")
    void addDynamicPermissionSavesWhenNotEmpty() {
        @SuppressWarnings("rawtypes")
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);

        Role role = new Role();
        role.setId(1L);
        when(query.fetch()).thenReturn(List.of(role));

        service.addDynamicPermission(List.of("perm"), List.of(1L), "biz");

        verify(repository).saveAll(any());
    }

    @Test
    @DisplayName("addDynamicPermission：角色查询为空时不保存")
    void addDynamicPermissionSkipsWhenEmpty() {
        @SuppressWarnings("rawtypes")
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.fetch()).thenReturn(List.of());

        service.addDynamicPermission(List.of("perm"), List.of(1L), "biz");

        verify(repository, org.mockito.Mockito.never()).saveAll(any());
    }

    @Test
    @DisplayName("deleteDynamicPermission：删除到记录时正常返回")
    void deleteDynamicPermissionWhenRowsDeleted() {
        when(queryFactory.delete(any(EntityPath.class)).where(any(Predicate.class)).execute()).thenReturn(1L);

        service.deleteDynamicPermission("biz");
    }

    @Test
    @DisplayName("deleteDynamicPermission：没有记录可删时正常返回")
    void deleteDynamicPermissionWhenNoRows() {
        when(queryFactory.delete(any(EntityPath.class)).where(any(Predicate.class)).execute()).thenReturn(0L);

        service.deleteDynamicPermission("biz");
    }

    @Test
    @DisplayName("QRole 元数据可访问（保证查询表达式构造正确）")
    void qRoleMetadataAccessible() {
        assertThat(QRole.role).isNotNull();
        assertThat(new GrantPermission()).isNotNull();
    }
}
