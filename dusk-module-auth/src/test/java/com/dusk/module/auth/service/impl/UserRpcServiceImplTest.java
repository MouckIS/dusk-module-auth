package com.dusk.module.auth.service.impl;

import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.enums.UserStatus;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.dto.CreateOrUpdateUserInput;
import com.dusk.module.auth.dto.GetUsersByOrgInput;
import com.dusk.module.auth.dto.UserFullListDto;
import com.dusk.module.auth.dto.UserInputDto;
import com.dusk.module.auth.dto.UserOrgDto;
import com.dusk.module.auth.dto.UserSimpleDto;
import com.dusk.module.auth.dto.orga.GetOrganizationUnitUsersInput;
import com.dusk.module.auth.dto.orga.OrganizationUnitDto;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserDto;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.dto.user.UserIdAndPermissionDto;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.manage.IUserManage;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.IRoleService;
import com.dusk.module.auth.service.IUserService;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserRpcServiceImpl} 单元测试（第一批：委托型与业务逻辑型方法）。
 * 该类以对 Dubbo 暴露的薄封装为主，测试重点在于委派正确性与纯逻辑分支。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserRpcServiceImplTest {

    @Mock
    private IUserService userService;
    @Mock
    private IOrganizationUnitService organizationUnitService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private IRoleService roleService;
    @Mock
    private IUserManage userManage;
    @Mock
    private TokenAuthManager tokenAuthManager;
    @Mock
    private IUserRepository repository;
    @Mock
    private EntityManager entityManager;

    private UserRpcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserRpcServiceImpl();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "em", entityManager);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "organizationUnitService", organizationUnitService);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "roleService", roleService);
        ReflectionTestUtils.setField(service, "userManage", userManage);
        ReflectionTestUtils.setField(service, "tokenAuthManager", tokenAuthManager);
    }

    @AfterEach
    void tearDown() {
        DataFilterContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static User user(Long id, EUnitType type, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setUserName("u" + id);
        user.setName("name" + id);
        user.setUserType(type);
        user.setUserStatus(status);
        return user;
    }

    private static Role role(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setRoleCode("C" + id);
        role.setRoleName(name);
        role.setUserRoles(new ArrayList<>());
        return role;
    }

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

    /** selectFrom(...).where(...).fetchFirst() 链式 mock。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubSelectFirst(Object result) {
        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.fetchFirst()).thenReturn(result);
    }

    // ------------------------------------------------------------------
    // 查询 / 映射
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getUsers(PagedAndSortedInputDto)：分页映射为全量用户列表")
    void getUsersPaged() {
        Page<User> page = new PageImpl<>(List.of(user(1L, EUnitType.Inner, UserStatus.OnJob)),
                PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResultDto<UserFullListDto> result = service.getUsers(new PagedAndSortedInputDto());

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getUserName()).isEqualTo("u1");
    }

    @Test
    @DisplayName("getUsers(UserInputDto)：转换入参后委派给用户服务")
    void getUsersByInputDto() {
        when(userService.getUsers(any())).thenReturn(new PageImpl<>(List.of(user(2L, EUnitType.Inner, UserStatus.OnJob))));

        PagedResultDto<UserFullListDto> result = service.getUsers(new UserInputDto());

        assertThat(result.getItems()).hasSize(1);
        verify(userService).getUsers(any());
    }

    @Test
    @DisplayName("getUsersForSync：附加组织 id 清单")
    void getUsersForSyncAttachesOrgIds() {
        User u = user(1L, EUnitType.Inner, UserStatus.OnJob);
        u.getOrganizationUnit().add(orgUnit(100L, "ORG", null));
        Page<User> page = new PageImpl<>(List.of(u), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        var result = service.getUsersForSync(new PagedAndSortedInputDto());

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getOrgIds()).containsExactly(100L);
    }

    @Test
    @DisplayName("getUserFullById：命中返回 DTO，未命中返回 null")
    void getUserFullByIdHandlesBoth() {
        when(repository.findById(1L)).thenReturn(Optional.of(user(1L, EUnitType.Inner, UserStatus.OnJob)));
        when(repository.findById(2L)).thenReturn(Optional.empty());

        assertThat(service.getUserFullById(1L)).isNotNull();
        assertThat(service.getUserFullById(2L)).isNull();
    }

    @Test
    @DisplayName("getUserFullByIds：按 id 集合查询并映射")
    void getUserFullByIdsMapsList() {
        when(repository.findByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(user(1L, EUnitType.Inner, UserStatus.OnJob)));

        assertThat(service.getUserFullByIds(List.of(1L, 2L))).hasSize(1);
    }

    @Test
    @DisplayName("getUserPermissions：userId 为空返回空集合，否则委派")
    void getUserPermissionsHandlesNullUserId() {
        assertThat(service.getUserPermissions(null)).isEmpty();

        when(userManage.getUserPermissions(1L)).thenReturn(List.of("p1"));
        assertThat(service.getUserPermissions(1L)).containsExactly("p1");
    }

    @Test
    @DisplayName("getUserIdsByNameLike：按名称模糊与在职内单位过滤")
    void getUserIdsByNameLike() {
        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(user(1L, EUnitType.Inner, UserStatus.OnJob)));

        assertThat(service.getUserIdsByNameLike("na")).containsExactly(1L);
    }

    @Test
    @DisplayName("getUsersByUserNameStartWith：默认仅内单位 / 显式类型")
    void getUsersByUserNameStartWith() {
        stubSelectFromList(List.of(user(1L, EUnitType.Inner, UserStatus.OnJob)));

        assertThat(service.getUsersByUserNameStartWith("u")).hasSize(1);
        assertThat(service.getUsersByUserNameStartWith("u", List.of(EUnitType.External))).hasSize(1);
    }

    @Test
    @DisplayName("getUserByUserName：命中返回 DTO，未命中返回 null")
    void getUserByUserName() {
        when(repository.findByUserName("u1")).thenReturn(Optional.of(user(1L, EUnitType.Inner, UserStatus.OnJob)));
        when(repository.findByUserName("none")).thenReturn(Optional.empty());

        assertThat(service.getUserByUserName("u1")).isNotNull();
        assertThat(service.getUserByUserName("none")).isNull();
    }

    @Test
    @DisplayName("getUserByName：重名抛异常、单条返回、无结果返回 null")
    void getUserByNameHandlesThreeCases() {
        when(repository.findByName("dup"))
                .thenReturn(List.of(user(1L, EUnitType.Inner, UserStatus.OnJob), user(2L, EUnitType.Inner, UserStatus.OnJob)));
        when(repository.findByName("one")).thenReturn(List.of(user(1L, EUnitType.Inner, UserStatus.OnJob)));
        when(repository.findByName("none")).thenReturn(List.of());

        assertThatThrownBy(() -> service.getUserByName("dup"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("出现重名的用户");
        assertThat(service.getUserByName("one")).isNotNull();
        assertThat(service.getUserByName("none")).isNull();
    }

    @Test
    @DisplayName("getUsersContainsName：默认仅内单位 / 显式类型")
    void getUsersContainsName() {
        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(user(1L, EUnitType.Inner, UserStatus.OnJob)));

        assertThat(service.getUsersContainsName("na")).hasSize(1);
        assertThat(service.getUsersContainsName("na", List.of(EUnitType.Inner))).hasSize(1);
    }

    @Test
    @DisplayName("getUserByWorkNumber：命中/未命中")
    void getUserByWorkNumber() {
        when(repository.findByWorkNumber("w1")).thenReturn(Optional.of(user(1L, EUnitType.Inner, UserStatus.OnJob)));
        when(repository.findByWorkNumber("none")).thenReturn(Optional.empty());

        assertThat(service.getUserByWorkNumber("w1")).isNotNull();
        assertThat(service.getUserByWorkNumber("none")).isNull();
    }

    @Test
    @DisplayName("getUserOrgByIds：映射组织信息")
    void getUserOrgByIds() {
        User u = user(1L, EUnitType.Inner, UserStatus.OnJob);
        u.getOrganizationUnit().add(orgUnit(100L, "ORG", null));
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(u));

        List<UserOrgDto> result = service.getUserOrgByIds(List.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDtos()).hasSize(1);
    }

    @Test
    @DisplayName("getUsersByOrg：组织 id 为空返回空集合，否则查询组织下用户")
    void getUsersByOrg() {
        GetUsersByOrgInput empty = new GetUsersByOrgInput();
        assertThat(service.getUsersByOrg(empty)).isEmpty();

        GetUsersByOrgInput input = new GetUsersByOrgInput();
        input.setOrgIds(List.of(100L));
        input.setDeepQuery(true);
        when(organizationUnitService.findUsers(any(GetOrganizationUnitUsersInput.class)))
                .thenReturn(new PageImpl<>(List.of(user(1L, EUnitType.Inner, UserStatus.OnJob))));

        assertThat(service.getUsersByOrg(input)).hasSize(1);
    }

    @Test
    @DisplayName("getByEmailAddressOrPhoneNo：命中/未命中")
    void getByEmailAddressOrPhoneNo() {
        stubSelectFirst(user(1L, EUnitType.Inner, UserStatus.OnJob));
        assertThat(service.getByEmailAddressOrPhoneNo("a@b.c")).isNotNull();

        stubSelectFirst(null);
        assertThat(service.getByEmailAddressOrPhoneNo("none")).isNull();
    }

    @Test
    @DisplayName("getUsersByRoleName：角色不存在返回空，存在则按类型与在职状态过滤")
    void getUsersByRoleNameFilters() {
        when(roleService.getRoleByRoleName("none")).thenReturn(null);
        assertThat(service.getUsersByRoleName("none")).isEmpty();

        Role r = role(1L, "adm");
        r.getUserRoles().add(user(1L, EUnitType.Inner, UserStatus.OnJob));
        r.getUserRoles().add(user(2L, EUnitType.External, UserStatus.OnJob));
        r.getUserRoles().add(user(3L, EUnitType.Inner, UserStatus.Dimission));
        when(roleService.getRoleByRoleName("adm")).thenReturn(r);

        assertThat(service.getUsersByRoleName("adm")).hasSize(1);
        assertThat(service.getUsersByRoleName("adm", List.of(EUnitType.Inner)))
                .extracting(UserFullListDto::getId)
                .containsExactly(1L);
    }

    @Test
    @DisplayName("getUsersByRoleName(filterByStation=false)：2 参重载忽略场站开关，3 参重载不附加场站过滤")
    void getUsersByRoleNameWithoutStationFilter() {
        Role r = role(1L, "adm");
        r.getUserRoles().add(user(1L, EUnitType.Inner, UserStatus.OnJob));
        when(roleService.getRoleByRoleName("adm")).thenReturn(r);
        stubSelectFromList(List.of(user(1L, EUnitType.Inner, UserStatus.OnJob)));

        assertThat(service.getUsersByRoleName("adm", false)).hasSize(1);
        assertThat(service.getUsersByRoleName("adm", false, List.of(EUnitType.Inner))).hasSize(1);
    }

    @Test
    @DisplayName("getUsersByRoleName(filterByStation=true)：按数据过滤上下文追加场站条件")
    void getUsersByRoleNameWithStationFilter() {
        DataFilterContextHolder.setDataFilterId("100,200");
        stubSelectFromList(List.of(user(1L, EUnitType.Inner, UserStatus.OnJob)));

        assertThat(service.getUsersByRoleName("adm", true, List.of(EUnitType.Inner))).hasSize(1);
    }

    @Test
    @DisplayName("getUsersByRoleIds：角色为 null 返回空，否则去重合并用户")
    void getUsersByRoleIds() {
        when(roleService.getRoleByIds(List.of(1L))).thenReturn(null);
        assertThat(service.getUsersByRoleIds(List.of(1L))).isEmpty();

        Role r1 = role(1L, "a");
        r1.getUserRoles().add(user(1L, EUnitType.Inner, UserStatus.OnJob));
        r1.getUserRoles().add(user(2L, EUnitType.Inner, UserStatus.OnJob));
        Role r2 = role(2L, "b");
        r2.getUserRoles().add(user(2L, EUnitType.Inner, UserStatus.OnJob));
        when(roleService.getRoleByIds(List.of(1L, 2L))).thenReturn(List.of(r1, r2));

        assertThat(service.getUsersByRoleIds(List.of(1L, 2L)))
                .extracting(UserFullListDto::getId)
                .containsExactly(1L, 2L);
    }

    // ------------------------------------------------------------------
    // 权限判定
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getUserIdsByPermissionsAnd：仅保留同时具备全部权限的用户")
    void getUserIdsByPermissionsAnd() {
        when(repository.getUserIdsByPermissionsAnd(any(), anyList())).thenReturn(List.of(
                new UserIdAndPermissionDto(1L, "p1"),
                new UserIdAndPermissionDto(1L, "p2"),
                new UserIdAndPermissionDto(2L, "p1")));

        assertThat(service.getUserIdsByPermissionsAnd(new String[]{"p1", "p2"})).containsExactly(1L);
        assertThat(service.getUserIdsByPermissionsAnd(new String[]{"p1", "p2"}, List.of(EUnitType.Inner)))
                .containsExactly(1L);
    }

    @Test
    @DisplayName("getUserIdsByPermissionsOr：委派仓储 OR 查询")
    void getUserIdsByPermissionsOr() {
        when(repository.getUserIdsByPermissions(any(), anyList())).thenReturn(List.of(1L, 2L));

        assertThat(service.getUserIdsByPermissionsOr(new String[]{"p1"})).containsExactly(1L, 2L);
        assertThat(service.getUserIdsByPermissionsOr(new String[]{"p1"}, List.of(EUnitType.Inner)))
                .containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("getUserIdsByPermission：单权限重载")
    void getUserIdsByPermission() {
        when(repository.getUserIdsByPermissions(any(), anyList())).thenReturn(List.of(1L));

        assertThat(service.getUserIdsByPermission("p1")).containsExactly(1L);
        assertThat(service.getUserIdsByPermission("p1", List.of(EUnitType.Inner))).isNull();
    }

    @Test
    @DisplayName("checkUserContainOneOfPermissions：命中任一为 true，permissions 为 null 返回 false")
    void checkUserContainOneOfPermissions() {
        when(userManage.getUserPermissions(1L)).thenReturn(List.of("p1"));

        assertThat(service.checkUserContainOneOfPermissions(1L, new String[]{"p1", "x"})).isTrue();
        assertThat(service.checkUserContainOneOfPermissions(1L, new String[]{"y"})).isFalse();
        assertThat(service.checkUserContainOneOfPermissions(1L, null)).isFalse();
    }

    @Test
    @DisplayName("checkUserContainPermissions：需包含全部才为 true，permissions 为 null 返回 false")
    void checkUserContainPermissions() {
        when(userManage.getUserPermissions(1L)).thenReturn(List.of("p1", "p2"));

        assertThat(service.checkUserContainPermissions(1L, new String[]{"p1", "p2"})).isTrue();
        assertThat(service.checkUserContainPermissions(1L, new String[]{"p1", "x"})).isFalse();
        assertThat(service.checkUserContainPermissions(1L, null)).isFalse();
        assertThat(service.checkUserContainPermission(1L, "p1")).isTrue();
    }

    // ------------------------------------------------------------------
    // 委派型写入 / Token
    // ------------------------------------------------------------------

    @Test
    @DisplayName("encodePassword / createOrUpdateUser / createOrUpdateUsers：委派正确")
    void delegationForWriteOperations() {
        when(passwordEncoder.encode("raw")).thenReturn("encoded");
        assertThat(service.encodePassword("raw")).isEqualTo("encoded");

        when(userService.createOrUpdateUser(any(CreateOrUpdateUserInput.class))).thenReturn(9L);
        assertThat(service.createOrUpdateUser(new CreateOrUpdateUserInput())).isEqualTo(9L);

        service.createOrUpdateUsers(List.of(new CreateOrUpdateUserInput(), new CreateOrUpdateUserInput()));
        verify(userService, org.mockito.Mockito.times(3)).createOrUpdateUser(any(CreateOrUpdateUserInput.class));
    }

    @Test
    @DisplayName("getSuperiorUserFullById / getSuperiorId / getCurrentRoles：委派用户服务")
    void delegationForSuperiorAndRoles() {
        when(userService.getSuperiorUserFullById(1L)).thenReturn(new UserFullListDto());
        when(userService.getSuperiorId(1L)).thenReturn(2L);
        when(userService.getCurrentRoles(1L)).thenReturn(List.of(5L));

        assertThat(service.getSuperiorUserFullById(1L)).isNotNull();
        assertThat(service.getSuperiorId(1L)).isEqualTo(2L);
        assertThat(service.getCurrentRoles(1L)).containsExactly(5L);
    }

    @Test
    @DisplayName("generateToken 系列：委派用户服务")
    void delegationForTokenGeneration() {
        when(userService.generateToken(1L)).thenReturn("t1");
        when(userService.generateTokenByUserName("u")).thenReturn("t2");
        when(userService.generateExpireTokenByUserName("u", 5L, TimeUnit.MINUTES)).thenReturn("t3");

        assertThat(service.generateToken(1L)).isEqualTo("t1");
        assertThat(service.generateTokenByUserName("u")).isEqualTo("t2");
        assertThat(service.generateExpireTokenByUserName("u", 5L, TimeUnit.MINUTES)).isEqualTo("t3");
    }

    @Test
    @DisplayName("removeToken：委派 TokenAuthManager")
    void removeTokenDelegates() {
        service.removeToken("auth");

        verify(tokenAuthManager).removeToken("auth");
    }

    @Test
    @DisplayName("deleteUser：包装为 EntityDto 后委派")
    void deleteUserDelegates() {
        service.deleteUser(3L);

        verify(userService).deleteUser(any(EntityDto.class));
    }

    @Test
    @DisplayName("totalCount：返回仓储计数")
    void totalCount() {
        when(repository.count()).thenReturn(7L);

        assertThat(service.totalCount()).isEqualTo(7L);
    }

    // ------------------------------------------------------------------
    // getUserSimpleDto 系列
    // ------------------------------------------------------------------

    @Test
    @DisplayName("loadAllUserList / getUserSimpleDto(long)：查询用户简要信息")
    void loadAllAndSingleUserSimpleDto() {
        stubSelectList(List.of(new UserSimpleDto()));
        assertThat(service.loadAllUserList()).hasSize(1);

        UserSimpleDto dto = new UserSimpleDto();
        stubSelectOne(dto);
        assertThat(service.getUserSimpleDto(1L)).isSameAs(dto);
    }

    @Test
    @DisplayName("getUserSimpleDto(Long...)：空入参返回空，单元素命中/未命中，多元素走集合查询")
    void getUserSimpleDtoVarargs() {
        assertThat(service.getUserSimpleDto((Long[]) null)).isEmpty();
        assertThat(service.getUserSimpleDto(new Long[0])).isEmpty();

        stubSelectOne(new UserSimpleDto());
        assertThat(service.getUserSimpleDto(new Long[]{1L})).hasSize(1);

        stubSelectOne(null);
        assertThat(service.getUserSimpleDto(new Long[]{2L})).isEmpty();

        stubSelectList(List.of(new UserSimpleDto()));
        assertThat(service.getUserSimpleDto(1L, 2L)).hasSize(1);
    }

    @Test
    @DisplayName("getUserSimpleDto(Collection)：空入参返回空，单元素命中/未命中，多元素走集合查询")
    void getUserSimpleDtoCollection() {
        assertThat(service.getUserSimpleDto((List<Long>) null)).isEmpty();
        assertThat(service.getUserSimpleDto(new ArrayList<Long>())).isEmpty();

        stubSelectOne(new UserSimpleDto());
        assertThat(service.getUserSimpleDto(List.of(1L))).hasSize(1);

        stubSelectOne(null);
        assertThat(service.getUserSimpleDto(List.of(2L))).isEmpty();

        stubSelectList(List.of(new UserSimpleDto()));
        assertThat(service.getUserSimpleDto(List.of(1L, 2L))).hasSize(1);
    }

    @Test
    @DisplayName("createUserSimpleQBean：可构造 QBean 投影")
    void createUserSimpleQBean() {
        assertThat(service.createUserSimpleQBean()).isNotNull();
    }

    // ------------------------------------------------------------------
    // 包级可见辅助方法
    // ------------------------------------------------------------------

    @Test
    @DisplayName("recursiveSaveOrganizationUnit：按层级递归保存并重排 sortIndex")
    void recursiveSaveOrganizationUnitRecurses() {
        OrganizationUnit root = orgUnit(1L, "C1", null);
        OrganizationUnit child = orgUnit(2L, "C2", 1L);

        service.recursiveSaveOrganizationUnit(1, root, List.of(root, child));

        assertThat(root.getSortIndex()).isEqualTo(1);
        assertThat(child.getSortIndex()).isEqualTo(1);
        verify(organizationUnitService, org.mockito.Mockito.times(2)).save(any(OrganizationUnit.class));
    }

    // ------------------------------------------------------------------
    // 同步（sync）相关
    // ------------------------------------------------------------------

    @Test
    @DisplayName("syncUserAndOrg：删除离职用户、重建组织树并批量保存用户")
    void syncUserAndOrgFullFlow() {
        stubDelete(1L);

        // 数据库已有 org=1，orgList 中命中后复用；org=2 为新增
        // 注意：生产代码会对 orgList 执行 iterator.remove()，故必须返回可变集合
        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.fetch()).thenReturn(
                (List) new ArrayList<>(List.of(orgUnit(1L, "OLD", null))),
                (List) new ArrayList<>(List.of(role(1L, "default"))));

        when(repository.findAll()).thenReturn(new ArrayList<>());

        List<OrganizationUnitDto> saveOrgList = List.of(
                orgDto(1L, "ORG1", null),
                orgDto(2L, "ORG2", 1L));
        UserSimpleDto userDto = new UserSimpleDto();
        userDto.setId(10L);
        userDto.setUserName("u10");
        userDto.setWorkNumber("w10");
        List<OrganizationUnitUserDto> saveOrgUserList = new ArrayList<>(
                List.of(new OrganizationUnitUserDto(1L, 10L)));
        when(passwordEncoder.encode("w10")).thenReturn("ENC");

        service.syncUserAndOrg(List.of(userDto), saveOrgList, saveOrgUserList, List.of(99L));

        // 组织树已保存
        verify(organizationUnitService, org.mockito.Mockito.times(2)).save(any(OrganizationUnit.class));
    }

    @Test
    @DisplayName("syncUserAndOrg：无离职用户时不触发删除语句")
    void syncUserAndOrgSkipsDeleteWhenNoResignedUsers() {
        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.fetch()).thenReturn((List) new ArrayList<>(), (List) new ArrayList<>());
        when(repository.findAll()).thenReturn(new ArrayList<>());

        service.syncUserAndOrg(new ArrayList<>(),
                List.of(orgDto(1L, "ORG1", null)),
                new ArrayList<>(),
                new ArrayList<>());

        verify(queryFactory, org.mockito.Mockito.never()).delete(any(EntityPath.class));
    }

    @Test
    @DisplayName("syncFromLdap：组织映射为空且用户列表为空时仅查询默认角色")
    void syncFromLdapWithEmptyInputs() {
        stubSelectFromList(List.of(role(1L, "default")));

        service.syncFromLdap(new ArrayList<>(), new ArrayList<>(), new java.util.HashMap<>());

        verify(organizationUnitService, org.mockito.Mockito.never()).saveAndFlush(any(OrganizationUnit.class));
    }

    @Test
    @DisplayName("syncFromLdap：按用户组织映射保存组织并同步用户")
    void syncFromLdapSyncsOrgAndUser() {
        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        // 生产代码会向 orgList 追加新组织，必须返回可变集合
        when(query.fetch()).thenReturn(
                (List) new ArrayList<>(),
                (List) new ArrayList<>(List.of(role(1L, "default"))),
                (List) new ArrayList<>(List.of(role(1L, "default"))));

        UserSimpleDto userDto = new UserSimpleDto();
        userDto.setId(10L);
        userDto.setUserName("u1");
        userDto.setWorkNumber("w1");
        when(passwordEncoder.encode("w1")).thenReturn("ENC");

        java.util.Map<String, List<OrganizationUnitDto>> userOrgMap =
                new java.util.HashMap<>();
        userOrgMap.put("u1", List.of(orgDto(null, "C1", null)));

        service.syncFromLdap(List.of(userDto), new ArrayList<>(), userOrgMap);

        verify(organizationUnitService).saveAndFlush(any(OrganizationUnit.class));
    }

    @Test
    @DisplayName("syncFromLdap：存在离职用户名时执行删除语句")
    void syncFromLdapDeletesResignedUsers() {
        stubDelete(1L);
        stubSelectFromList(List.of(role(1L, "default")));

        service.syncFromLdap(new ArrayList<>(), List.of("resigned"), new java.util.HashMap<>());

        verify(queryFactory).delete(any(EntityPath.class));
    }

    @Test
    @DisplayName("syncUserByWeChatFromLdap：用户已存在时更新并绑定组织")
    void syncUserByWeChatFromLdapUpdatesExistingUser() {
        when(repository.findById(10L))
                .thenReturn(Optional.of(user(10L, EUnitType.Inner, UserStatus.OnJob)));
        when(organizationUnitService.findById(100L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");

        UserSimpleDto userDto = new UserSimpleDto();
        userDto.setId(10L);
        userDto.setUserName("u10");
        userDto.setWorkNumber("w10");

        OrganizationUnitDto unitDto = orgDto(100L, "ORG", null);
        OrganizationUnitUserDto orgUserDto = new OrganizationUnitUserDto(100L, 10L);

        service.syncUserByWeChatFromLdap(userDto, List.of(unitDto), orgUserDto);

        verify(repository).save(any(User.class));
    }

    @Test
    @DisplayName("syncUserByWeChatFromLdap：用户不存在时新建并赋予默认角色")
    void syncUserByWeChatFromLdapCreatesNewUser() {
        when(repository.findById(11L)).thenReturn(Optional.empty());
        when(organizationUnitService.findById(100L)).thenReturn(Optional.of(orgUnit(100L, "ORG", null)));
        stubSelectFromList(List.of(role(1L, "default")));

        UserSimpleDto userDto = new UserSimpleDto();
        userDto.setId(11L);
        userDto.setUserName("u11");
        userDto.setWorkNumber("w11");

        service.syncUserByWeChatFromLdap(userDto, List.of(orgDto(100L, "ORG", null)),
                new OrganizationUnitUserDto(100L, 11L));

        verify(repository).save(any(User.class));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubDelete(long executed) {
        com.querydsl.jpa.impl.JPADeleteClause clause = mock(com.querydsl.jpa.impl.JPADeleteClause.class);
        when(queryFactory.delete(any(EntityPath.class))).thenReturn(clause);
        when(clause.where(any(Predicate.class))).thenReturn(clause);
        when(clause.execute()).thenReturn(executed);
    }

    private static OrganizationUnitDto orgDto(Long id, String code, Long parentId) {
        OrganizationUnitDto dto = new OrganizationUnitDto();
        dto.setId(id);
        dto.setCode(code);
        dto.setDisplayName("org" + code);
        dto.setParentId(parentId);
        return dto;
    }

    // ------------------------------------------------------------------

    private static OrganizationUnit orgUnit(Long id, String code, Long parentId) {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setId(id);
        unit.setCode(code);
        unit.setDisplayName("org" + id);
        unit.setParentId(parentId);
        return unit;
    }
}
