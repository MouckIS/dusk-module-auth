package com.dusk.module.auth.service.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.dto.BindRoleToUserInput;
import com.dusk.module.auth.dto.RoleSimpleDto;
import com.dusk.module.auth.dto.orga.BindRoleToOrgInput;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.module.auth.dto.role.*;
import com.dusk.module.auth.dto.user.GetUserByRoleDto;
import com.dusk.module.auth.dto.user.UnbindRoleForUserDto;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.repository.IRoleRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.ITenantPermissionService;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link RoleServiceImpl} 单元测试，目标：覆盖 service.impl 门禁包的分支与行。
 *
 * <p>说明：{@code exportRole} 的 Excel 写出由 EasyExcel 静态入口驱动。当前依赖树中 commons-io 被解析为 2.5
 * （经 weixin-java-miniapp 传递），低于 easyexcel 4.0.3 要求的 2.12，运行期可能抛
 * {@code NoClassDefFoundError: org/apache/commons/io/output/UnsynchronizedByteArrayOutputStream}。
 * 因此导出用例通过 {@code mockStatic(EasyExcelFactory.class)} 隔离真实写出，只验证权限树折叠、
 * 模板填充与资源关闭等编排逻辑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleServiceImplTest {

    private static final Long ROLE_ID = 5L;
    private static final Long TENANT_ID = 7L;

    @Mock
    private IRoleRepository repository;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private IOrganizationUnitService organizationUnitService;
    @Mock
    private IGrantPermissionRepository permissionRepository;
    @Mock
    private IAuthPermissionManager authPermissionManager;
    @Mock
    private ITenantPermissionService tenantPermissionService;
    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private com.dusk.module.auth.authz.version.AuthzPermissionVersionService authzPermissionVersionService;
    @Mock
    private com.dusk.module.auth.authz.version.AuthzSubjectVersionService authzSubjectVersionService;
    @Mock
    private com.dusk.module.auth.authz.service.AuthzEventPublisher authzEventPublisher;
    @Mock
    private com.dusk.module.auth.authz.service.AuthzEventPublisher authzSubjectEventPublisher;

    private RoleServiceImpl service;

    private static void evaluateSpecification(Specification<Role> specification) {
        Root<Role> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);

        assertThatCode(() -> specification.toPredicate(root, query, builder)).doesNotThrowAnyException();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    /**
     * {@code RoleServiceImpl} 自行声明了 {@code private IRoleRepository repository}，
     * 与 {@link BaseService} 的 {@code protected repository} 形成字段遮蔽。
     * {@link ReflectionTestUtils#setField} 只会命中子类字段，故父类字段需显式注入。
     */
    private void injectRepository(IRoleRepository repo) {
        ReflectionTestUtils.setField(service, "repository", repo);
        Field baseField = ReflectionUtils.findField(BaseService.class, "repository");
        ReflectionUtils.makeAccessible(baseField);
        ReflectionUtils.setField(baseField, service, repo);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static Role role(Long id, String code, String name) {
        Role role = new Role();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setUserRoles(new ArrayList<>());
        return role;
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("u" + id);
        return user;
    }

    private static GrantPermission grant(String name, String businessKey) {
        GrantPermission gp = new GrantPermission();
        gp.setName(name);
        gp.setBusinessKey(businessKey);
        return gp;
    }

    private static Permission permission(String name) {
        return new Permission(name, name + "-display", MultiTenancySides.Host);
    }

    private static RolePermissionDto permDto(String name, String displayName, String parentName) {
        RolePermissionDto dto = new RolePermissionDto();
        dto.setName(name);
        dto.setDisplayName(displayName);
        dto.setParentName(parentName);
        return dto;
    }

    private static RolePermissionDto granted(String name, String displayName, String parentName) {
        RolePermissionDto dto = permDto(name, displayName, parentName);
        dto.setGranted(true);
        return dto;
    }

    /** selectFrom(...).where(...) 链式 mock。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPAQuery queryMock() {
        JPAQuery query = mock(JPAQuery.class);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.from(any(EntityPath.class))).thenReturn(query);
        return query;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubSelectFromList(List<?> result) {
        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.fetch()).thenReturn((List) result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubSelectList(List<?> result) {
        JPAQuery query = queryMock();
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.fetch()).thenReturn((List) result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubSelectOne(Object result) {
        JPAQuery query = queryMock();
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.fetchOne()).thenReturn(result);
    }

    /** 桩化 selectFrom(...).where(...).fetchCount()，用于角色代码/名称唯一性校验。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubFetchCount(long... counts) {
        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        if (counts.length == 1) {
            when(query.fetchCount()).thenReturn(counts[0]);
        } else {
            when(query.fetchCount()).thenReturn(counts[0], counts[1]);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubDelete(long executed) {
        JPADeleteClause clause = mock(JPADeleteClause.class);
        when(queryFactory.delete(any(EntityPath.class))).thenReturn(clause);
        when(clause.where(any(Predicate.class))).thenReturn(clause);
        when(clause.execute()).thenReturn(executed);
    }

    // ------------------------------------------------------------------
    // getRoles
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getRoles：返回仓储全量数据")
    void getRolesReturnsAll() {
        when(repository.findAll()).thenReturn(List.of(role(1L, "R", "N")));

        assertThat(service.getRoles()).hasSize(1);
    }

    @Test
    @DisplayName("getRoles(GetRolesInput)：带过滤条件返回分页结果")
    void getRolesWithFilterReturnsPage() {
        Page<Role> page = new PageImpl<>(List.of(role(1L, "R", "N")), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        GetRolesInput input = new GetRolesInput();
        input.setFilter("adm");

        assertThat(service.getRoles(input).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getRoles(GetRolesInput)：过滤条件为空时同样返回分页结果")
    void getRolesWithoutFilterReturnsPage() {
        Page<Role> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        assertThat(service.getRoles(new GetRolesInput()).getTotalElements()).isZero();
    }

    // ------------------------------------------------------------------
    // createOrUpdate
    // ------------------------------------------------------------------

    @Test
    @DisplayName("createOrUpdate：无 id 时新建角色")
    void createOrUpdateCreatesWhenIdNull() {
        stubFetchCount(0L);
        when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleCreateOrEditDto dto = new RoleCreateOrEditDto();
        dto.setRoleCode("NEW");
        dto.setRoleName("新角色");

        Role saved = service.createOrUpdate(dto);

        assertThat(saved.getRoleCode()).isEqualTo("NEW");
        assertThat(saved.getRoleName()).isEqualTo("新角色");
        verify(repository).save(any(Role.class));
    }

    @Test
    @DisplayName("createOrUpdate：带 id 时更新已有角色")
    void createOrUpdateUpdatesWhenIdNotNull() {
        stubFetchCount(0L);
        Role existing = role(ROLE_ID, "OLD", "旧角色");
        when(repository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleCreateOrEditDto dto = new RoleCreateOrEditDto();
        dto.setId(ROLE_ID);
        dto.setRoleCode("UPDATED");
        dto.setRoleName("已更新");

        Role saved = service.createOrUpdate(dto);

        assertThat(saved.getRoleCode()).isEqualTo("UPDATED");
        assertThat(saved.getRoleName()).isEqualTo("已更新");
    }

    @Test
    @DisplayName("createOrUpdate：角色不存在时抛业务异常")
    void createOrUpdateThrowsWhenRoleNotFound() {
        when(repository.findById(ROLE_ID)).thenReturn(Optional.empty());

        RoleCreateOrEditDto dto = new RoleCreateOrEditDto();
        dto.setId(ROLE_ID);
        dto.setRoleCode("X");
        dto.setRoleName("X");

        assertThatThrownBy(() -> service.createOrUpdate(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("此角色不存在");
    }

    @Test
    @DisplayName("createOrUpdate：角色代码重复时抛业务异常")
    void createOrUpdateThrowsWhenRoleCodeDuplicated() {
        stubFetchCount(1L);

        RoleCreateOrEditDto dto = new RoleCreateOrEditDto();
        dto.setRoleCode("DUP");
        dto.setRoleName("任意");

        assertThatThrownBy(() -> service.createOrUpdate(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色代码已存在");
    }

    @Test
    @DisplayName("createOrUpdate：角色名称重复时抛业务异常")
    void createOrUpdateThrowsWhenRoleNameDuplicated() {
        stubFetchCount(0L, 1L);

        RoleCreateOrEditDto dto = new RoleCreateOrEditDto();
        dto.setRoleCode("OK");
        dto.setRoleName("DUP");

        assertThatThrownBy(() -> service.createOrUpdate(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色名称已存在");
    }

    // ------------------------------------------------------------------
    // validateRoleCodeAndNameUnique (包级可见，直接驱动分支)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("validateRoleCodeAndNameUnique：唯一时通过，且 id 非空时追加排除自身条件")
    void validateUniquePassesWithId() {
        stubFetchCount(0L);
        Role role = role(ROLE_ID, "R", "N");

        assertThatCode(() -> service.validateRoleCodeAndNameUnique(role)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateRoleCodeAndNameUnique：新建场景（id 为 null）同样通过")
    void validateUniquePassesWithoutId() {
        stubFetchCount(0L);
        Role role = role(null, "R", "N");

        assertThatCode(() -> service.validateRoleCodeAndNameUnique(role)).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------------
    // updatePermission
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updatePermission：角色 id 为空时抛业务异常")
    void updatePermissionThrowsWhenIdNull() {
        assertThatThrownBy(() -> service.updatePermission(new UpdateRolePermissionDto()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ID不可为空");
    }

    @Test
    @DisplayName("updatePermission：角色不存在时抛业务异常")
    void updatePermissionThrowsWhenRoleNotFound() {
        UpdateRolePermissionDto dto = new UpdateRolePermissionDto();
        dto.setId(ROLE_ID);
        when(repository.findById(ROLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePermission(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("此角色不存在");
    }

    @Test
    @DisplayName("updatePermission：仅替换无来源权限，保留业务来源权限并刷新缓存")
    void updatePermissionReplacesPermissions() {
        stubDelete(1L);

        Role existing = role(ROLE_ID, "R", "N");
        existing.addPermission(grant("OLD", null));
        existing.addPermission(grant("BIZ", "biz-1"));
        when(repository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRolePermissionDto dto = new UpdateRolePermissionDto();
        dto.setId(ROLE_ID);
        CreateOrEditRolePermissionDto p = new CreateOrEditRolePermissionDto();
        p.setName("NEW");
        dto.getPermissions().add(p);

        Role saved = service.updatePermission(dto);

        assertThat(saved.getPermissions()).extracting(GrantPermission::getName)
                .containsExactlyInAnyOrder("BIZ", "NEW");
        assertThat(saved.getPermissions()).allSatisfy(gp -> assertThat(gp.getRole()).isEqualTo(saved));
        verify(authPermissionManager).refreshAll();
    }

    // ------------------------------------------------------------------
    // getRoleDetails
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getRoleDetails：id 为空时抛业务异常")
    void getRoleDetailsThrowsWhenIdNull() {
        assertThatThrownBy(() -> service.getRoleDetails(new EntityDto()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ID不可为空");
    }

    @Test
    @DisplayName("getRoleDetails：角色不存在时抛业务异常")
    void getRoleDetailsThrowsWhenNotFound() {
        when(repository.findById(ROLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRoleDetails(new EntityDto(ROLE_ID)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("此角色不存在");
    }

    @Test
    @DisplayName("getRoleDetails：宿主侧（无租户上下文）从权限定义树取候选并标记授权")
    void getRoleDetailsHostScope() {
        Role existing = role(ROLE_ID, "R", "N");
        existing.addPermission(grant("A", null));
        existing.addPermission(grant("B", "biz-1"));
        when(repository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(authPermissionManager.getDefinitionPermissionTree(false))
                .thenReturn(List.of(permission("A"), permission("C")));

        RoleDto dto = service.getRoleDetails(new EntityDto(ROLE_ID));

        assertThat(dto.getRoleName()).isEqualTo("N");
        assertThat(dto.getPermissionList()).hasSize(2);
        assertThat(dto.getPermissionList())
                .filteredOn(RolePermissionDto::isGranted)
                .extracting(RolePermissionDto::getName)
                .containsExactly("A");
    }

    @Test
    @DisplayName("getRoleDetails：租户侧候选权限来自租户授权清单，且仅保留已授权项")
    void getRoleDetailsTenantScope() {
        TenantContextHolder.setTenantId(TENANT_ID);

        Role existing = role(ROLE_ID, "R", "N");
        existing.addPermission(grant("A", null));
        existing.addPermission(grant("B", "biz-1"));
        when(repository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        RolePermissionDto grantedA = granted("A", "A-display", null);
        RolePermissionDto notGrantedC = permDto("C", "C-display", null);
        when(tenantPermissionService.getTenantPermissions(TENANT_ID))
                .thenReturn(List.of(grantedA, notGrantedC));

        RoleDto dto = service.getRoleDetails(new EntityDto(ROLE_ID));

        // 仅 grantedA 进入租户授权清单，故候选集只有一个元素
        assertThat(dto.getPermissionList()).hasSize(1);
        assertThat(dto.getPermissionList().getFirst().getName()).isEqualTo("A");
        assertThat(dto.getPermissionList().getFirst().isGranted()).isTrue();
    }

    // ------------------------------------------------------------------
    // deleteRole
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleteRole：删除角色并刷新权限缓存")
    void deleteRoleDeletesAndRefreshes() {
        Role existing = role(ROLE_ID, "R", "N");
        when(repository.findById(ROLE_ID)).thenReturn(Optional.of(existing));

        service.deleteRole(new EntityDto(ROLE_ID));

        verify(repository).delete(existing);
        verify(authPermissionManager).refreshAll();
    }

    @Test
    @DisplayName("deleteRole：角色不存在时抛业务异常")
    void deleteRoleThrowsWhenNotFound() {
        when(repository.findById(ROLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRole(new EntityDto(ROLE_ID)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("此角色不存在");
    }

    // ------------------------------------------------------------------
    // importRole / batchImportRole
    // ------------------------------------------------------------------

    @Test
    @DisplayName("importRole：角色不存在时新建，并按授权清单写入权限")
    void importRoleCreatesNewRole() {
        stubFetchCount(0L);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());
        when(authPermissionManager.getDefinitionPermissionTree(false))
                .thenReturn(List.of(permission("P"), permission("C")));

        RoleDto dto = new RoleDto();
        dto.setRoleCode("NEW");
        dto.setRoleName("新角色");
        dto.getPermissionList().add(granted("P", "P-display", null));
        dto.getPermissionList().add(granted("C", "C-display", "P"));

        service.importRole(dto);

        verify(repository).save(any(Role.class));
        verify(permissionRepository).deleteInBatch(any());
        verify(authPermissionManager).refreshAll();
    }

    @Test
    @DisplayName("importRole：角色已存在时复用实体，移除未授权权限并补齐新增权限")
    void importRoleUpdatesExistingRoleAndRemovesStalePermissions() {
        stubFetchCount(0L);
        Role existing = role(ROLE_ID, "R", "N");
        existing.addPermission(grant("C", null));
        existing.addPermission(grant("X", null));
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(existing));
        when(authPermissionManager.getDefinitionPermissionTree(false))
                .thenReturn(List.of(permission("P"), permission("C")));

        RoleDto dto = new RoleDto();
        dto.setRoleCode("R");
        dto.setRoleName("N");
        dto.getPermissionList().add(granted("C", "C-display", null));
        dto.getPermissionList().add(granted("P", "P-display", null));

        service.importRole(dto);

        assertThat(existing.getPermissions()).extracting(GrantPermission::getName)
                .containsExactlyInAnyOrder("C", "P");

        ArgumentCaptor<List<GrantPermission>> captor = ArgumentCaptor.forClass(List.class);
        verify(permissionRepository).deleteInBatch(captor.capture());
        assertThat(captor.getValue()).extracting(GrantPermission::getName).containsExactly("X");
        verify(authPermissionManager).refreshAll();
    }

    @Test
    @DisplayName("importRole：名称重复时抛业务异常")
    void importRoleThrowsWhenNameDuplicated() {
        stubFetchCount(1L);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        RoleDto dto = new RoleDto();
        dto.setRoleCode("R");
        dto.setRoleName("DUP");

        assertThatThrownBy(() -> service.importRole(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色代码已存在");
        verify(repository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("batchImportRole：入参为 null 或空集合时直接返回")
    void batchImportRoleSkipsWhenNullOrEmpty() {
        service.batchImportRole(null);
        service.batchImportRole(new ArrayList<>());

        verifyNoInteractions(repository, permissionRepository, authPermissionManager);
    }

    @Test
    @DisplayName("batchImportRole：逐个委派给 importRole")
    void batchImportRoleDelegatesEachItem() {
        doNothing().when(service).importRole(any(RoleDto.class));

        RoleDto first = new RoleDto();
        first.setRoleName("A");
        RoleDto second = new RoleDto();
        second.setRoleName("B");

        service.batchImportRole(List.of(first, second));

        verify(service, times(2)).importRole(any(RoleDto.class));
    }

    // ------------------------------------------------------------------
    // 简单查询方法
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getRoleByRoleName：命中时返回实体，未命中返回 null")
    void getRoleByRoleNameHandlesBothCases() {
        when(repository.findByRoleName("R")).thenReturn(Optional.of(role(1L, "R", "N")));
        when(repository.findByRoleName("NONE")).thenReturn(Optional.empty());

        assertThat(service.getRoleByRoleName("R")).isNotNull();
        assertThat(service.getRoleByRoleName("NONE")).isNull();
    }

    @Test
    @DisplayName("getRoleByIds：按 id 集合查询")
    void getRoleByIdsDelegates() {
        when(repository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(role(1L, "R1", "N1")));

        assertThat(service.getRoleByIds(List.of(1L, 2L))).hasSize(1);
    }

    @Test
    @DisplayName("getRoleIdByRoleName：命中返回 id，未命中返回 null")
    void getRoleIdByRoleNameHandlesBothCases() {
        when(repository.findByRoleName("R")).thenReturn(Optional.of(role(9L, "R", "N")));
        when(repository.findByRoleName("NONE")).thenReturn(Optional.empty());

        assertThat(service.getRoleIdByRoleName("R")).isEqualTo(9L);
        assertThat(service.getRoleIdByRoleName("NONE")).isNull();
    }

    @Test
    @DisplayName("getRoles(PagedAndSortedInputDto)：分页映射为 RoleListDto")
    void getRolesPagedMapsToDto() {
        Page<Role> page = new PageImpl<>(List.of(role(1L, "R", "N")), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        assertThat(service.getRoles(new com.dusk.common.core.dto.PagedAndSortedInputDto()).getItems())
                .extracting(RoleListDto::getRoleCode)
                .containsExactly("R");
    }

    @Test
    @DisplayName("getRolesForSync：宿主侧附加权限清单（授权定义树）")
    void getRolesForSyncHostScope() {
        Role r = role(1L, "R", "N");
        r.addPermission(grant("A", null));
        Page<Role> page = new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Pageable.class))).thenReturn(page);
        when(authPermissionManager.getDefinitionPermissionTree(false))
                .thenReturn(List.of(permission("A"), permission("C")));

        var result = service.getRolesForSync(new com.dusk.common.core.dto.PagedAndSortedInputDto());

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getPermissionList()).hasSize(2);
    }

    @Test
    @DisplayName("getRolesForSync：租户侧权限清单来自租户授权")
    void getRolesForSyncTenantScope() {
        TenantContextHolder.setTenantId(TENANT_ID);
        Role r = role(1L, "R", "N");
        r.addPermission(grant("A", null));
        Page<Role> page = new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Pageable.class))).thenReturn(page);
        when(tenantPermissionService.getTenantPermissions(TENANT_ID))
                .thenReturn(List.of(granted("A", "A-display", null)));

        var result = service.getRolesForSync(new com.dusk.common.core.dto.PagedAndSortedInputDto());

        assertThat(result.getItems().getFirst().getPermissionList()).hasSize(1);
    }

    @Test
    @DisplayName("getRolesByIds：id 为 null 或空集合时抛业务异常")
    void getRolesByIdsThrowsOnEmptyInput() {
        assertThatThrownBy(() -> service.getRolesByIds(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("参数ID不能为空");
        assertThatThrownBy(() -> service.getRolesByIds(new ArrayList<>()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("参数ID不能为空");
    }

    @Test
    @DisplayName("getRolesByIds：正常返回并附加权限清单")
    void getRolesByIdsReturnsMappedList() {
        Role r = role(1L, "R", "N");
        r.addPermission(grant("A", null));
        stubSelectFromList(List.of(r));
        when(authPermissionManager.getDefinitionPermissionTree(false))
                .thenReturn(List.of(permission("A"), permission("C")));

        List<RoleListDto> result = service.getRolesByIds(List.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPermissionList()).hasSize(2);
    }

    @Test
    @DisplayName("getByRoleNames：按角色名查询")
    void getByRoleNamesQueriesByName() {
        RoleSimpleDto dto = new RoleSimpleDto();
        dto.setRoleName("R");
        stubSelectList(List.of(dto));

        assertThat(service.getByRoleNames(List.of("R"))).hasSize(1);
    }

    @Test
    @DisplayName("getByRoleCodes：按角色代码查询")
    void getByRoleCodesQueriesByCode() {
        RoleSimpleDto dto = new RoleSimpleDto();
        dto.setRoleCode("R");
        stubSelectList(List.of(dto));

        assertThat(service.getByRoleCodes(List.of("R"))).hasSize(1);
    }

    @Test
    @DisplayName("listDefaultRoles：返回默认角色列表")
    void listDefaultRolesReturnsMapped() {
        stubSelectFromList(List.of(role(1L, "R", "N")));

        assertThat(service.listDefaultRoles()).hasSize(1);
    }

    @Test
    @DisplayName("getDefaultRoles：返回默认角色简要信息")
    void getDefaultRolesReturnsSimpleDto() {
        stubSelectList(List.of(new RoleSimpleDto()));

        assertThat(service.getDefaultRoles()).hasSize(1);
    }

    @Test
    @DisplayName("getDefaultRoleIds：返回默认角色 id 集合")
    void getDefaultRoleIdsReturnsIds() {
        stubSelectList(List.of(1L, 2L));

        assertThat(service.getDefaultRoleIds()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("findByCodes：入参为空返回空集合")
    void findByCodesReturnsEmptyWhenBlank() {
        assertThat(service.findByCodes(null)).isEmpty();
        assertThat(service.findByCodes(new ArrayList<>())).isEmpty();
    }

    @Test
    @DisplayName("findByCodes：正常查询返回实体集合")
    void findByCodesReturnsEntities() {
        stubSelectFromList(List.of(role(1L, "R", "N")));

        assertThat(service.findByCodes(List.of("R"))).hasSize(1);
    }

    @Test
    @DisplayName("totalCount：返回仓储计数")
    void totalCountReturnsRepositoryCount() {
        when(repository.count()).thenReturn(3L);

        assertThat(service.totalCount()).isEqualTo(3L);
    }

    // ------------------------------------------------------------------
    // 角色-用户绑定
    // ------------------------------------------------------------------

    @Test
    @DisplayName("bindRoleToUsers：角色不存在时抛业务异常")
    void bindRoleToUsersThrowsWhenRoleMissing() {
        doReturn(null).when(service).getOne(anyLong());

        BindRoleToUserInput input = new BindRoleToUserInput();
        input.setRoleId(ROLE_ID);
        input.setUserIds(List.of(10L));

        assertThatThrownBy(() -> service.bindRoleToUsers(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("找不到对应角色");
    }

    @Test
    @DisplayName("bindRoleToUsers：仅绑定尚未关联的用户")
    void bindRoleToUsersSkipsAlreadyBound() {
        User bound = user(10L);
        User fresh = user(20L);
        Role role = role(ROLE_ID, "R", "N");
        role.getUserRoles().add(bound);
        doReturn(role).when(service).getOne(ROLE_ID);
        when(userRepository.findAllById(any())).thenReturn(List.of(bound, fresh));

        BindRoleToUserInput input = new BindRoleToUserInput();
        input.setRoleId(ROLE_ID);
        input.setUserIds(List.of(10L, 20L));

        service.bindRoleToUsers(input);

        assertThat(role.getUserRoles()).containsExactly(bound, fresh);
        verify(repository).save(role);
    }

    @Test
    @DisplayName("bindRoleToOrgans：角色不存在时抛业务异常")
    void bindRoleToOrgansThrowsWhenRoleMissing() {
        doReturn(null).when(service).getOne(anyLong());

        BindRoleToOrgInput input = new BindRoleToOrgInput() {
            @Override
            public Long getRoleId() {
                return ROLE_ID;
            }
        };

        assertThatThrownBy(() -> service.bindRoleToOrgans(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("找不到对应角色");
    }

    @Test
    @DisplayName("bindRoleToOrgans：将组织下用户批量绑定到角色")
    void bindRoleToOrgansBindsUsersOfOrgans() {
        Role role = role(ROLE_ID, "R", "N");
        doReturn(role).when(service).getOne(ROLE_ID);

        OrganizationUnitUserListDto orgUser = new OrganizationUnitUserListDto();
        orgUser.setId(20L);
        when(organizationUnitService.getOrganizationUnitUsers(
                any(), any(), anyBoolean(), any(Long[].class)))
                .thenReturn(new PagedResultDto<>(1L, List.of(orgUser)));
        when(userRepository.findAllById(any())).thenReturn(List.of(user(20L)));

        BindRoleToOrgInput input = new BindRoleToOrgInput() {
            @Override
            public Long getRoleId() {
                return ROLE_ID;
            }

            @Override
            public List<Long> getOrgIds() {
                return List.of(100L);
            }

            @Override
            public boolean isIncludeChild() {
                return true;
            }
        };

        service.bindRoleToOrgans(input);

        assertThat(role.getUserRoles()).extracting(User::getId).containsExactly(20L);
        verify(repository).save(role);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    @DisplayName("getUserByRoleId：角色 id 为空时抛业务异常")
    void getUserByRoleIdThrowsWhenRoleIdNull() {
        assertThatThrownBy(() -> service.getUserByRoleId(new GetUserByRoleDto()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色id不能为空");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    @DisplayName("getUserByRoleId：带用户名与用户类型过滤")
    void getUserByRoleIdAppliesFilters() {
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.selectDistinct(any(Expression.class))).thenReturn(query);
        when(query.from(any(EntityPath.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);

        Page<User> page = new PageImpl<>(List.of(user(1L)));
        doReturn(page).when(service).page(any(JPAQuery.class), any(Pageable.class));

        GetUserByRoleDto input = new GetUserByRoleDto();
        input.setRoleId(ROLE_ID);
        input.setUserName("  bob  ");
        input.setUserType(EUnitType.Inner);

        assertThat(service.getUserByRoleId(input)).isSameAs(page);
        // 1 次主条件 + 用户名 + 用户类型
        verify(query, times(3)).where(any(Predicate.class));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    @DisplayName("getUserByRoleId：无过滤条件时仅按角色关联查询")
    void getUserByRoleIdWithoutFilters() {
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.selectDistinct(any(Expression.class))).thenReturn(query);
        when(query.from(any(EntityPath.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);

        Page<User> page = new PageImpl<>(List.of());
        doReturn(page).when(service).page(any(JPAQuery.class), any(Pageable.class));

        GetUserByRoleDto input = new GetUserByRoleDto();
        input.setRoleId(ROLE_ID);

        assertThat(service.getUserByRoleId(input)).isSameAs(page);
        verify(query, times(1)).where(any(Predicate.class));
    }

    @Test
    @DisplayName("unbindRoleToUser：解绑指定用户并保存")
    void unbindRoleToUserRemovesBoundUser() {
        User bound = user(10L);
        Role role = role(ROLE_ID, "R", "N");
        role.getUserRoles().add(bound);
        doReturn(role).when(service).getOne(ROLE_ID);

        UnbindRoleForUserDto dto = new UnbindRoleForUserDto();
        dto.setRoleId(ROLE_ID);
        dto.setUserIds(List.of(10L));

        service.unbindRoleToUser(dto);

        assertThat(role.getUserRoles()).isEmpty();
        verify(repository).save(role);
    }

    // ------------------------------------------------------------------
    // deleteByCode
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleteByCode：删除成功时刷新权限缓存")
    void deleteByCodeRefreshesWhenDeleted() {
        stubDelete(1L);

        service.deleteByCode("R");

        verify(authPermissionManager).refreshAll();
    }

    @Test
    @DisplayName("deleteByCode：无匹配记录时不刷新缓存")
    void deleteByCodeSkipsRefreshWhenNothingDeleted() {
        stubDelete(0L);

        service.deleteByCode("R");

        verify(authPermissionManager, never()).refreshAll();
    }

    // ------------------------------------------------------------------
    // getRoleSimple 系列
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getRoleSimple(long)：返回单条简要信息")
    void getRoleSimpleByIdReturnsDto() {
        RoleSimpleDto dto = new RoleSimpleDto();
        stubSelectOne(dto);

        assertThat(service.getRoleSimple(1L)).isSameAs(dto);
    }

    @Test
    @DisplayName("getRoleSimple(Long...)：空入参返回空集合")
    void getRoleSimpleVarargsReturnsEmptyWhenBlank() {
        assertThat(service.getRoleSimple((Long[]) null)).isEmpty();
        assertThat(service.getRoleSimple(new Long[0])).isEmpty();
    }

    @Test
    @DisplayName("getRoleSimple(Long...)：单元素命中时返回集合，未命中返回空集合")
    void getRoleSimpleVarargsSingleElement() {
        RoleSimpleDto dto = new RoleSimpleDto();
        stubSelectOne(dto);
        assertThat(service.getRoleSimple(new Long[]{1L})).hasSize(1);

        stubSelectOne(null);
        assertThat(service.getRoleSimple(new Long[]{2L})).isEmpty();
    }

    @Test
    @DisplayName("getRoleSimple(Long...)：多元素走集合查询")
    void getRoleSimpleVarargsMultiElement() {
        stubSelectList(List.of(new RoleSimpleDto(), new RoleSimpleDto()));

        assertThat(service.getRoleSimple(1L, 2L)).hasSize(2);
    }

    @Test
    @DisplayName("getRoleSimple(Collection)：空入参返回空集合")
    void getRoleSimpleCollectionReturnsEmptyWhenBlank() {
        assertThat(service.getRoleSimple((List<Long>) null)).isEmpty();
        assertThat(service.getRoleSimple(new ArrayList<Long>())).isEmpty();
    }

    @Test
    @DisplayName("getRoleSimple(Collection)：单元素命中/未命中")
    void getRoleSimpleCollectionSingleElement() {
        RoleSimpleDto dto = new RoleSimpleDto();
        stubSelectOne(dto);
        assertThat(service.getRoleSimple(List.of(1L))).hasSize(1);

        stubSelectOne(null);
        assertThat(service.getRoleSimple(List.of(2L))).isEmpty();
    }

    @Test
    @DisplayName("getRoleSimple(Collection)：多元素走集合查询")
    void getRoleSimpleCollectionMultiElement() {
        stubSelectList(List.of(new RoleSimpleDto()));

        assertThat(service.getRoleSimple(List.of(1L, 2L))).hasSize(1);
    }

    @Test
    @DisplayName("createRoleSimpleQBean：可构造 QBean 投影")
    void createRoleSimpleQBeanBuildsProjection() {
        assertThat(service.createRoleSimpleQBean()).isNotNull();
    }

    // ------------------------------------------------------------------
    // 包级可见的辅助方法
    // ------------------------------------------------------------------

    @Test
    @DisplayName("appendRolePermissionExcelDataList：按层级缩进并深度优先展开")
    void appendRolePermissionExcelDataListIndentsByLevel() {
        ExportRolePermissionDto parent = new ExportRolePermissionDto(permDto("P", "Parent", null));
        ExportRolePermissionDto child = new ExportRolePermissionDto(permDto("C", "Child", "P"));
        ExportRolePermissionDto grand = new ExportRolePermissionDto(permDto("G", "Grand", "C"));
        child.appendChild(grand);
        parent.appendChild(child);

        List<ExportRolePermissionDto> dataList = new ArrayList<>();
        service.appendRolePermissionExcelDataList(dataList, parent, 0);

        assertThat(dataList).containsExactly(parent, child, grand);
        assertThat(dataList).extracting(ExportRolePermissionDto::getDisplayName)
                .containsExactly("Parent", "    Child", "        Grand");
    }

    @Test
    @DisplayName("appendTenantPermission：命中时递归补齐父级权限")
    void appendTenantPermissionAddsSelfAndParents() {
        Set<String> set = new HashSet<>();
        List<RolePermissionDto> tenantPermissions = List.of(
                permDto("child", "Child", "parent"),
                permDto("parent", "Parent", null));

        service.appendTenantPermission("child", set, tenantPermissions);

        assertThat(set).containsExactlyInAnyOrder("child", "parent");
    }

    @Test
    @DisplayName("appendTenantPermission：未命中时不写入集合")
    void appendTenantPermissionSkipsWhenMissing() {
        Set<String> set = new HashSet<>();
        List<RolePermissionDto> tenantPermissions = List.of(permDto("child", "Child", "parent"));

        service.appendTenantPermission("missing", set, tenantPermissions);

        assertThat(set).isEmpty();
    }

    // ------------------------------------------------------------------
    // 过滤规格求值与权限事件
    // ------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        service = spy(new RoleServiceImpl());
        injectRepository(repository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "organizationUnitService", organizationUnitService);
        ReflectionTestUtils.setField(service, "permissionRepository", permissionRepository);
        ReflectionTestUtils.setField(service, "authPermissionManager", authPermissionManager);
        ReflectionTestUtils.setField(service, "tenantPermissionService", tenantPermissionService);
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "authzPermissionVersionService", authzPermissionVersionService);
        ReflectionTestUtils.setField(service, "authzSubjectVersionService", authzSubjectVersionService);
        ReflectionTestUtils.setField(service, "authzEventPublisher", authzEventPublisher);
        ReflectionTestUtils.setField(service, "authzSubjectEventPublisher", authzSubjectEventPublisher);
    }

    @Test
    @DisplayName("getRoles(GetRolesInput)：带过滤条件时规格谓词可正常求值")
    void getRolesWithFilterSpecificationEvaluated() {
        Page<Role> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        GetRolesInput input = new GetRolesInput();
        input.setFilter("adm");

        service.getRoles(input);

        ArgumentCaptor<Specification<Role>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(captor.capture(), any(Pageable.class));
        evaluateSpecification(captor.getValue());
    }

    @Test
    @DisplayName("getRoles(GetRolesInput)：过滤条件为空时规格谓词同样可求值")
    void getRolesWithoutFilterSpecificationEvaluated() {
        Page<Role> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        GetRolesInput input = new GetRolesInput();
        input.setFilter("   ");

        service.getRoles(input);

        ArgumentCaptor<Specification<Role>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(captor.capture(), any(Pageable.class));
        evaluateSpecification(captor.getValue());
    }

    @Test
    @DisplayName("publishPermissionChanged：角色为 null 时直接跳过版本分配")
    void publishPermissionChangedSkipsNullRole() {
        ReflectionTestUtils.invokeMethod(service, "publishPermissionChanged", (Role) null);

        verifyNoInteractions(authzPermissionVersionService, authzEventPublisher);
    }

    @Test
    @DisplayName("publishPermissionChanged：角色无 id 时跳过版本分配")
    void publishPermissionChangedSkipsRoleWithoutId() {
        Role roleWithoutId = role(null, "R", "N");
        when(repository.findById(null)).thenReturn(Optional.of(roleWithoutId));

        service.deleteRole(new EntityDto());

        verifyNoInteractions(authzPermissionVersionService, authzEventPublisher);
    }

    @Test
    @DisplayName("unbindRoleToUser：用户 id 不匹配时保留原有绑定")
    void unbindRoleToUserKeepsUnmatchedUser() {
        User bound = user(10L);
        Role role = role(ROLE_ID, "R", "N");
        role.getUserRoles().add(bound);
        doReturn(role).when(service).getOne(ROLE_ID);
        when(userRepository.findAllById(any())).thenReturn(List.of());

        UnbindRoleForUserDto dto = new UnbindRoleForUserDto();
        dto.setRoleId(ROLE_ID);
        dto.setUserIds(List.of(99L));

        service.unbindRoleToUser(dto);

        assertThat(role.getUserRoles()).containsExactly(bound);
        verify(repository).save(role);
    }

    // ------------------------------------------------------------------
    // exportRole
    // ------------------------------------------------------------------

    @Test
    @DisplayName("bumpSubjects：跳过 null 用户与无 id 用户，仅推进有效用户版本")
    void bumpSubjectsSkipsInvalidUsers() {
        Role role = role(ROLE_ID, "R", "N");
        doReturn(role).when(service).getOne(ROLE_ID);
        User valid = user(1L);
        User withoutId = user(null);
        when(userRepository.findAllById(any())).thenReturn(java.util.Arrays.asList(null, withoutId, valid));

        UnbindRoleForUserDto dto = new UnbindRoleForUserDto();
        dto.setRoleId(ROLE_ID);
        dto.setUserIds(List.of(1L));

        service.unbindRoleToUser(dto);

        verify(authzSubjectVersionService, times(1)).nextVersion(any(), any());
        verify(authzSubjectEventPublisher, times(1)).subjectChanged(any(), any(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("exportRole：折叠权限树后按模板填充并关闭 ExcelWriter")
    void exportRoleFillsTemplateAndClosesWriter() {
        TenantContextHolder.setTenantId(TENANT_ID);
        Role existing = role(ROLE_ID, "R", "N");
        existing.addPermission(grant("P", null));
        when(repository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(tenantPermissionService.getTenantPermissions(TENANT_ID))
                .thenReturn(List.of(granted("P", "P-display", null), granted("C", "C-display", "P")));

        try (MockedStatic<EasyExcelFactory> excel = mockStatic(EasyExcelFactory.class)) {
            ExcelWriter writer = mock(ExcelWriter.class);
            ExcelWriterBuilder builder = mock(ExcelWriterBuilder.class, Answers.RETURNS_SELF);
            when(builder.build()).thenReturn(writer);
            ExcelWriterSheetBuilder sheetBuilder = mock(ExcelWriterSheetBuilder.class, Answers.RETURNS_SELF);
            when(sheetBuilder.build()).thenReturn(mock(WriteSheet.class));
            excel.when(() -> EasyExcelFactory.write(any(OutputStream.class))).thenReturn(builder);
            excel.when(() -> EasyExcelFactory.writerSheet(0)).thenReturn(sheetBuilder);

            service.exportRole(ROLE_ID, new ByteArrayOutputStream());

            verify(writer, times(2)).fill(any(Object.class), any(WriteSheet.class));
            verify(writer).finish();
        }
    }

    @Test
    @DisplayName("exportRole：Excel 写出失败时仍执行资源清理并向上抛出")
    void exportRoleClosesStreamWhenWriteFails() {
        Role existing = role(ROLE_ID, "R", "N");
        when(repository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(authPermissionManager.getDefinitionPermissionTree(false)).thenReturn(List.of());

        try (MockedStatic<EasyExcelFactory> excel = mockStatic(EasyExcelFactory.class)) {
            excel.when(() -> EasyExcelFactory.write(any(OutputStream.class)))
                    .thenThrow(new IllegalStateException("excel"));

            assertThatThrownBy(() -> service.exportRole(ROLE_ID, new ByteArrayOutputStream()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("excel");
        }
    }
}
