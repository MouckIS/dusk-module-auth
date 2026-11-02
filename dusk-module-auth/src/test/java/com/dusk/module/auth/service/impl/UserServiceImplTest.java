package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.enums.UserStatus;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.exception.UserLoginException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.common.mqs.core.MessageSender;
import com.dusk.common.mqs.pusher.SmsPushConfig;
import com.dusk.module.auth.common.config.AppAuthConfig;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.util.LoginUtils;
import com.dusk.module.auth.dto.ChangePwdInput;
import com.dusk.module.auth.dto.CreateOrUpdateUserInput;
import com.dusk.module.auth.dto.UserEditDto;
import com.dusk.module.auth.dto.orga.GetOrganizationUnitUsersInput;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.dto.user.*;
import com.dusk.module.auth.entity.*;
import com.dusk.module.auth.enums.ELevel;
import com.dusk.module.auth.feature.UserFeatureProvider;
import com.dusk.module.auth.mapper.RoleMapper;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.repository.IOrganizationManagerRepository;
import com.dusk.module.auth.repository.ITenantRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.IFeatureChecker;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.IRoleService;
import com.dusk.module.auth.service.IStationService;
import com.dusk.module.metadata.service.ISettingRpcService;
import com.dusk.module.notification.service.IAuthPushRpcService;
import com.dusk.module.notification.service.IEmailRpcService;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyBoolean;

/**
 * {@link UserServiceImpl} 单元测试。
 *
 * <p>约定与说明：
 * <ul>
 *   <li>{@code appAuthConfig.getLoginEncryptKey()} 返回 null，使 {@code decryptSm4} 恒走异常分支并原样返回入参，
 *       避免依赖真实国密密钥；</li>
 *   <li>{@code roleService.findAll()} 默认返回空集合：MapStruct 生成的 {@code RoleMapperImpl#toRoleDto}
 *       未映射 {@code roleId}，非空角色列表会触发 {@code userRoleDto.getRoleId().equals(...)} 空指针，
 *       该现象属既有实现约束，测试以空集合规避。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock
    private IGrantPermissionRepository grantPermissionRepository;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IRoleService roleService;
    @Mock
    private IOrganizationUnitService organizationUnitService;
    @Mock
    private IFeatureChecker featureChecker;
    @Mock
    private RedisUtil<Object> redisUtil;
    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private ITenantRepository tenantRepository;
    @Mock
    private AppAuthConfig appAuthConfig;
    @Mock
    private SmsPushConfig smsPushConfig;
    @Mock
    private IAuthPushRpcService pushRpcService;
    @Mock
    private IEmailRpcService emailRpcService;
    @Mock
    private TokenAuthManager tokenAuthManager;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private IOrganizationManagerRepository organizationManagerRepository;
    @Mock
    private IStationService stationService;
    @Mock
    private ISettingRpcService settingRpcService;
    @Mock
    private MessageSender sender;
    @Mock
    private EntityManager entityManager;
    @Mock
    private com.dusk.module.auth.authz.version.AuthzSubjectVersionService authzSubjectVersionService;
    @Mock
    private com.dusk.module.auth.authz.service.AuthzEventPublisher authzEventPublisher;

    private UserServiceImpl service;

    /**
     * 触发 {@link Specification#toPredicate}，让 {@code Specifications.where(...)} 中的 lambda 真正执行。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void evaluateSpecification(Specification<?> specification) {
        Root root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Join join = mock(Join.class);
        jakarta.persistence.criteria.Predicate conjunction =
                mock(jakarta.persistence.criteria.Predicate.class);
        when(root.get(anyString())).thenReturn(path);
        when(root.join(anyString(), any(JoinType.class))).thenReturn(join);
        when(join.get(anyString())).thenReturn(path);
        when(conjunction.getExpressions()).thenReturn(new ArrayList<>());
        when(builder.conjunction()).thenReturn(conjunction);
        when(builder.disjunction()).thenReturn(conjunction);

        ((Specification) specification).toPredicate(root, query, builder);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        LoginUserIdContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("name" + id);
        user.setUserName("user" + id);
        user.setActive(true);
        user.setUserStatus(UserStatus.OnJob);
        user.setAccessFailedCount(0);
        user.setUserRoles(new ArrayList<>());
        return user;
    }

    private static OrganizationUnit orgUnit(Long id, Long parentId) {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setId(id);
        unit.setDisplayName("org" + id);
        unit.setParentId(parentId);
        return unit;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPAQuery queryMock() {
        JPAQuery query = mock(JPAQuery.class);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.from(any(EntityPath.class))).thenReturn(query);
        when(query.distinct()).thenReturn(query);
        when(query.leftJoin(any(EntityPath.class))).thenReturn(query);
        when(query.on(any(Predicate.class))).thenReturn(query);
        return query;
    }

    private void stubManagerQuery(List<OrganizationUnit> result) {
        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.fetch()).thenReturn(result);
    }

    // ------------------------------------------------------------------
    // checkAndGetUser / checkUserValid / checkUserIsActive
    // ------------------------------------------------------------------

    @Test
    @DisplayName("checkAndGetUser：账户不存在时抛 UsernameNotFoundException")
    void checkAndGetUserThrowsWhenUserMissing() {
        when(userRepository.findByUserName("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkAndGetUser("none", "pwd"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("checkAndGetUser：账户存在且校验通过时清零失败次数并保存")
    void checkAndGetUserReturnsUserAndResetsFailedCount() {
        User user = activeUser(1L);
        user.setAccessFailedCount(5);
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));

        User result = service.checkAndGetUser("user1", "pwd");

        assertThat(result).isSameAs(user);
        assertThat(result.getAccessFailedCount()).isZero();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("checkAndGetUser：租户上下文开启忽略大小写特性时按忽略大小写查询")
    void checkAndGetUserUsesIgnoreCaseWhenFeatureEnabled() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.isEnabled(com.dusk.module.auth.feature.LoginFeatureProvider.APP_LOGIN_IGNORE_CASE))
                .thenReturn(true);
        when(userRepository.findByUserNameIgnoreCase("User1")).thenReturn(Optional.of(activeUser(1L)));

        assertThat(service.checkAndGetUser("User1", "pwd")).isNotNull();
        verify(userRepository).findByUserNameIgnoreCase("User1");
    }

    @Test
    @DisplayName("checkUserValid：账户锁定未到期时抛锁定异常")
    void checkUserValidThrowsWhenLocked() {
        User user = activeUser(1L);
        user.setLockoutEndDateUtc(LocalDateTime.now().plusHours(2));

        assertThatThrownBy(() -> service.checkUserValid(user))
                .isInstanceOf(UserLoginException.class)
                .hasMessageContaining("锁定");
    }

    @Test
    @DisplayName("checkUserValid：已离职用户抛离职异常")
    void checkUserValidThrowsWhenDimission() {
        User user = activeUser(1L);
        user.setUserStatus(UserStatus.Dimission);

        assertThatThrownBy(() -> service.checkUserValid(user))
                .isInstanceOf(UserLoginException.class)
                .hasMessageContaining("离职");
    }

    @Test
    @DisplayName("checkUserValid：未激活用户抛未激活异常")
    void checkUserValidThrowsWhenNotActive() {
        User user = activeUser(1L);
        user.setActive(false);

        assertThatThrownBy(() -> service.checkUserValid(user))
                .isInstanceOf(UserLoginException.class);
    }

    @Test
    @DisplayName("checkUserValid：租户不存在时抛业务异常")
    void checkUserValidThrowsWhenTenantMissing() {
        User user = activeUser(1L);
        user.setTenantId(9L);
        when(tenantRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkUserValid(user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账户不可用");
    }

    @Test
    @DisplayName("checkUserValid：租户已禁用时抛不可用异常")
    void checkUserValidThrowsWhenTenantDisabled() {
        User user = activeUser(1L);
        user.setTenantId(9L);
        Tenant tenant = new Tenant();
        tenant.setActive(false);
        when(tenantRepository.findById(9L)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.checkUserValid(user))
                .isInstanceOf(UserLoginException.class)
                .hasMessageContaining("不可用");
    }

    @Test
    @DisplayName("checkUserValid：宿主有效用户通过校验")
    void checkUserValidPassesForHostUser() {
        assertThatCode(() -> service.checkUserValid(activeUser(1L))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkUserValid：租户有效时回填 tenant 并通过校验")
    void checkUserValidPassesAndBindsTenant() {
        User user = activeUser(1L);
        user.setTenantId(9L);
        Tenant tenant = new Tenant();
        tenant.setActive(true);
        when(tenantRepository.findById(9L)).thenReturn(Optional.of(tenant));

        service.checkUserValid(user);

        assertThat(user.getTenant()).isSameAs(tenant);
    }

    @Test
    @DisplayName("checkUserIsActive：按激活状态与生效区间判定")
    void checkUserIsActiveBranches() {
        User user = activeUser(1L);
        assertThat(service.checkUserIsActive(user)).isTrue();

        user.setActiveStartDate(LocalDate.now().plusDays(1));
        assertThat(service.checkUserIsActive(user)).isFalse();

        user.setActiveStartDate(null);
        user.setActiveEndDate(LocalDate.now().minusDays(2));
        assertThat(service.checkUserIsActive(user)).isFalse();

        user.setActiveEndDate(LocalDate.now().plusDays(1));
        assertThat(service.checkUserIsActive(user)).isTrue();

        user.setActive(false);
        assertThat(service.checkUserIsActive(user)).isFalse();
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getUserById：命中返回实体，未命中抛业务异常")
    void getUserByIdHandlesBoth() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(service.getUserById(1L)).isNotNull();
        assertThatThrownBy(() -> service.getUserById(2L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getAllUsers / getAllInnerUsers：分别走仓储与 QueryDSL")
    void getAllUsersAndInnerUsers() {
        when(userRepository.findAll()).thenReturn(List.of(activeUser(1L)));
        assertThat(service.getAllUsers()).hasSize(1);

        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.fetch()).thenReturn(List.of(activeUser(1L)));
        assertThat(service.getAllInnerUsers()).hasSize(1);
    }

    @Test
    @DisplayName("getCurrentRoles：返回用户角色 id 集合")
    void getCurrentRoles() {
        User user = activeUser(1L);
        Role role = new Role();
        role.setId(5L);
        user.getUserRoles().add(role);
        when(userRepository.getOne(1L)).thenReturn(user);

        assertThat(service.getCurrentRoles(1L)).containsExactly(5L);
    }

    @Test
    @DisplayName("getUsersForLogin：按名称/拼音/账号条件查询并映射")
    void getUsersForLogin() {
        when(userRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(activeUser(1L)));

        GetUsersForLoginInput input = new GetUsersForLoginInput();
        input.setName("na");
        input.setSurName("su");
        input.setUserName("user");

        assertThat(service.getUsersForLogin(input)).hasSize(1);
    }

    @Test
    @DisplayName("getUsers：按条件构造规格并分页查询")
    void getUsersPaged() {
        Page<User> page = new PageImpl<>(List.of(activeUser(1L)), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        assertThat(service.getUsers(new GetUsersInput()).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getUsersList：映射列表并回填锁定与激活状态")
    void getUsersList() {
        User locked = activeUser(1L);
        locked.setLockoutEndDateUtc(LocalDateTime.now().plusHours(3));
        Page<User> page = new PageImpl<>(List.of(locked), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var result = service.getUsersList(new GetUsersInput());

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().isLock()).isTrue();
        assertThat(result.getItems().getFirst().isActive()).isTrue();
    }

    @Test
    @DisplayName("getOrgaUsers：机构不存在抛异常；存在时按机构/角色过滤分页")
    void getOrgaUsers() {
        GetOrgaUsersInput missing = new GetOrgaUsersInput();
        missing.setOrgaId(99L);
        when(organizationUnitService.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getOrgaUsers(missing)).isInstanceOf(BusinessException.class);

        OrganizationUnit orga = orgUnit(1L, null);
        when(organizationUnitService.findById(1L)).thenReturn(Optional.of(orga));
        Role role = new Role();
        role.setId(7L);
        when(roleService.findById(7L)).thenReturn(Optional.of(role));
        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        Page<User> page = new PageImpl<>(List.of(activeUser(1L)));
        doReturn(page).when(service).page(any(), any());

        GetOrgaUsersInput input = new GetOrgaUsersInput();
        input.setOrgaId(1L);
        input.setRoleId(7L);
        input.setFilter("na");

        assertThat(service.getOrgaUsers(input)).isSameAs(page);
    }

    @Test
    @DisplayName("getOrgaUsers：按角色名解析角色")
    void getOrgaUsersByRoleName() {
        when(organizationUnitService.findById(1L)).thenReturn(Optional.of(orgUnit(1L, null)));
        Role role = new Role();
        role.setId(7L);
        when(roleService.getRoleByRoleName("adm")).thenReturn(role);
        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        Page<User> page = new PageImpl<>(List.of(activeUser(1L)));
        doReturn(page).when(service).page(any(), any());

        GetOrgaUsersInput input = new GetOrgaUsersInput();
        input.setOrgaId(1L);
        input.setRoleName("adm");

        assertThat(service.getOrgaUsers(input)).isSameAs(page);
    }

    @Test
    @DisplayName("getUsersByRoleCodes：非全角色模式走 distinct 查询")
    void getUsersByRoleCodesWithoutAllRoles() {
        JPAQuery roleQuery = queryMock();
        when(queryFactory.select(any(Expression.class))).thenReturn(roleQuery);
        when(roleQuery.fetch()).thenReturn(List.of(1L));
        JPAQuery userQuery = mock(JPAQuery.class);
        when(queryFactory.selectDistinct(any(Expression.class))).thenReturn(userQuery);
        when(userQuery.from(any(EntityPath.class))).thenReturn(userQuery);
        when(userQuery.where(any(Predicate.class))).thenReturn(userQuery);
        Page<User> page = new PageImpl<>(List.of(activeUser(1L)));
        doReturn(page).when(service).page(any(), any());

        GetUsersByRoleCodesInput input = new GetUsersByRoleCodesInput();
        input.setRoleCodeList(List.of("C1"));
        input.setUserType(EUnitType.Inner);

        assertThat(service.getUsersByRoleCodes(input)).isSameAs(page);
    }

    @Test
    @DisplayName("getUsersByRoleName：按角色名解析角色 id 后分页查询")
    void getUsersByRoleName() {
        JPAQuery roleQuery = queryMock();
        when(queryFactory.select(any(Expression.class))).thenReturn(roleQuery);
        when(roleQuery.fetch()).thenReturn(List.of(1L));
        JPAQuery userQuery = mock(JPAQuery.class);
        when(queryFactory.selectDistinct(any(Expression.class))).thenReturn(userQuery);
        when(userQuery.from(any(EntityPath.class))).thenReturn(userQuery);
        when(userQuery.where(any(Predicate.class))).thenReturn(userQuery);
        Page<User> page = new PageImpl<>(List.of(activeUser(1L)));
        doReturn(page).when(service).page(any(), any());

        GetUsersByRoleNameInput input = new GetUsersByRoleNameInput();
        input.setRoleNames(List.of("adm"));
        input.setUserType(EUnitType.Inner);

        assertThat(service.getUsersByRoleName(input)).isSameAs(page);
    }

    // ------------------------------------------------------------------
    // 上级 / 组织
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getSuperiorId：无组织返回 null；命中管理层返回其 userId")
    void getSuperiorIdReturnsManagerUserId() {
        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(new ArrayList<>());
        assertThat(service.getSuperiorId(1L)).isNull();

        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(List.of(orgUnit(1L, null)));
        OrganizationManager manager = new OrganizationManager();
        manager.setUserId(7L);
        when(organizationManagerRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.of(manager));

        assertThat(service.getSuperiorId(1L)).isEqualTo(7L);
    }

    @Test
    @DisplayName("getSuperiorId：当前层无管理层时沿父节点递归，父节点为根则返回 null")
    void getSuperiorIdRecursesToParent() {
        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(List.of(orgUnit(1L, 2L)));
        when(organizationManagerRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(organizationUnitService.findById(2L)).thenReturn(Optional.of(orgUnit(2L, null)));

        assertThat(service.getSuperiorId(1L)).isNull();
    }

    @Test
    @DisplayName("getSuperiorUserFullById：上级不存在返回 null，存在则返回 DTO")
    void getSuperiorUserFullById() {
        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(new ArrayList<>());
        assertThat(service.getSuperiorUserFullById(1L)).isNull();

        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(List.of(orgUnit(1L, null)));
        OrganizationManager manager = new OrganizationManager();
        manager.setUserId(7L);
        when(organizationManagerRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.of(manager));
        when(userRepository.findById(7L)).thenReturn(Optional.of(activeUser(7L)));

        assertThat(service.getSuperiorUserFullById(1L)).isNotNull();
    }

    // ------------------------------------------------------------------
    // 密码与状态
    // ------------------------------------------------------------------

    @Test
    @DisplayName("saveUserPicture：签名/头像分别写入，非法类型抛异常")
    void saveUserPictureBranches() {
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.saveUserPicture(1L, 11L, "signature");
        assertThat(user.getSignaturePictureId()).isEqualTo(11L);

        service.saveUserPicture(1L, 12L, "profile");
        assertThat(user.getProfilePictureId()).isEqualTo(12L);

        assertThatThrownBy(() -> service.saveUserPicture(1L, 13L, "other"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("参数传递不正确");
        verify(userRepository, times(2)).save(user);
    }

    @Test
    @DisplayName("unlockUser：清零失败次数并清除锁定时间")
    void unlockUser() {
        User user = activeUser(1L);
        user.setAccessFailedCount(9);
        user.setLockoutEndDateUtc(LocalDateTime.now().plusHours(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.unlockUser(new EntityDto(1L));

        assertThat(user.getAccessFailedCount()).isZero();
        assertThat(user.getLockoutEndDateUtc()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword：新旧密码相同时抛异常")
    void changePasswordThrowsWhenSameAsOld() {
        User user = activeUser(1L);
        user.setPassword("HASH");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("np", "HASH")).thenReturn(true);

        ChangePasswordInput cpi = new ChangePasswordInput();
        cpi.setNewPasswd("np");
        cpi.setOldPasswd("op");
        UserContext context = new UserContext();
        context.setId(1L);

        assertThatThrownBy(() -> service.changePassword(cpi, context))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("新密码不能与旧密码相同");
    }

    @Test
    @DisplayName("changePassword：旧密码错误时抛异常")
    void changePasswordThrowsWhenOldPasswordWrong() {
        User user = activeUser(1L);
        user.setPassword("HASH");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("np", "HASH")).thenReturn(false);
        when(passwordEncoder.matches("op", "HASH")).thenReturn(false);

        ChangePasswordInput cpi = new ChangePasswordInput();
        cpi.setNewPasswd("np");
        cpi.setOldPasswd("op");
        UserContext context = new UserContext();
        context.setId(1L);

        assertThatThrownBy(() -> service.changePassword(cpi, context))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码错误");
    }

    @Test
    @DisplayName("changePassword：校验通过后更新密码并清零失败次数")
    void changePasswordSucceeds() {
        User user = activeUser(1L);
        user.setPassword("HASH");
        user.setAccessFailedCount(3);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("np", "HASH")).thenReturn(false);
        when(passwordEncoder.matches("op", "HASH")).thenReturn(true);
        when(passwordEncoder.encode("np")).thenReturn("NEWHASH");

        ChangePasswordInput cpi = new ChangePasswordInput();
        cpi.setNewPasswd("np");
        cpi.setOldPasswd("op");
        UserContext context = new UserContext();
        context.setId(1L);

        service.changePassword(cpi, context);

        assertThat(user.getPassword()).isEqualTo("NEWHASH");
        assertThat(user.isShouldChangePasswordOnNextLogin()).isFalse();
        assertThat(user.getAccessFailedCount()).isZero();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("listChangePassword：新旧密码相同抛异常；否则重置为需改密状态")
    void listChangePassword() {
        User user = activeUser(1L);
        user.setPassword("HASH");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("np", "HASH")).thenReturn(true);

        ChangePwdInput same = new ChangePwdInput();
        same.setUserId(1L);
        same.setNewPwd("np");
        assertThatThrownBy(() -> service.listChangePassword(same)).isInstanceOf(BusinessException.class);

        when(passwordEncoder.matches("np2", "HASH")).thenReturn(false);
        when(passwordEncoder.encode("np2")).thenReturn("H2");
        ChangePwdInput ok = new ChangePwdInput();
        ok.setUserId(1L);
        ok.setNewPwd("np2");
        service.listChangePassword(ok);

        assertThat(user.getPassword()).isEqualTo("H2");
        assertThat(user.isShouldChangePasswordOnNextLogin()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changeStatus：正常状态仅保存，离职状态额外投递取消授权消息")
    void changeStatus() {
        User user = activeUser(1L);
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));

        ChangeStatusInput onJob = new ChangeStatusInput();
        onJob.setUserIds(List.of(1L));
        onJob.setStatus(UserStatus.OnJob);
        service.changeStatus(onJob);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.OnJob);
        verify(sender, never()).sendAsync(any());

        ChangeStatusInput dimission = new ChangeStatusInput();
        dimission.setUserIds(List.of(1L));
        dimission.setStatus(UserStatus.Dimission);
        service.changeStatus(dimission);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.Dimission);
        verify(sender).sendAsync(any());
    }

    @Test
    @DisplayName("activeUser：验证码命中时抛异常；否则激活用户")
    void activeUser() {
        when(redisUtil.getCache("k1")).thenReturn("c1");
        assertThatThrownBy(() -> service.activeUser(1L, "k1", "c1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证失败");

        when(redisUtil.getCache("k2")).thenReturn("other");
        User user = activeUser(1L);
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.activeUser(1L, "k2", "c1");

        assertThat(user.isActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("setDefaultStation：厂站不存在抛异常，存在则写入默认厂站")
    void setDefaultStation() {
        UserContext context = new UserContext();
        context.setId(1L);
        when(securityUtils.getCurrentUser()).thenReturn(context);

        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(stationService.findById(99L)).thenReturn(Optional.empty());
        SetDefaultStationInput missing = new SetDefaultStationInput();
        missing.setStationId(99L);
        assertThatThrownBy(() -> service.setDefaultStation(missing)).isInstanceOf(BusinessException.class);

        when(stationService.findById(5L)).thenReturn(Optional.of(mock(Station.class)));
        SetDefaultStationInput ok = new SetDefaultStationInput();
        ok.setStationId(5L);

        service.setDefaultStation(ok);

        assertThat(user.getDefaultStation()).isEqualTo(5L);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updatePersonalInfo：更新个人信息并同步组织与管理关系")
    void updatePersonalInfo() {
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        stubManagerQuery(new ArrayList<>());

        PersonalInfoInput input = new PersonalInfoInput();
        input.setId(1L);
        input.setName("newName");
        input.setOrgaId(10L);
        input.setManagerOrgIds(List.of(10L));

        service.updatePersonalInfo(input);

        assertThat(user.getName()).isEqualTo("newName");
        verify(organizationManagerRepository).deleteByUserId(1L);
        verify(organizationManagerRepository).saveAll(any());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deleteUser：删除用户并投递取消授权消息；不存在抛异常")
    void deleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteUser(new EntityDto(1L)))
                .isInstanceOf(BusinessException.class);

        User user = activeUser(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        service.deleteUser(new EntityDto(2L));

        verify(userRepository).delete(user);
        verify(organizationManagerRepository).deleteByUserId(2L);
        verify(sender).sendAsync(any());
    }

    @Test
    @DisplayName("deleteUsers：入参为空抛异常；否则排除当前登录人后批量删除")
    void deleteUsers() {
        assertThatThrownBy(() -> service.deleteUsers(new ArrayList<>()))
                .isInstanceOf(BusinessException.class);

        LoginUserIdContextHolder.setUserId(99L);
        List<EntityDto> dtos = new ArrayList<>(List.of(new EntityDto(1L), new EntityDto(2L)));
        service.deleteUsers(dtos);

        verify(userRepository).deleteByIdIn(List.of(1L, 2L));
        verify(organizationManagerRepository).deleteByUserIdIn(List.of(1L, 2L));
    }

    @Test
    @DisplayName("deleteUserByIds：空入参直接返回，否则执行删除语句")
    void deleteUserByIds() {
        service.deleteUserByIds(null);
        service.deleteUserByIds(new ArrayList<>());
        verify(queryFactory, never()).delete(any(EntityPath.class));

        JPAQuery deleteStub = mock(JPAQuery.class);
        com.querydsl.jpa.impl.JPADeleteClause clause = mock(com.querydsl.jpa.impl.JPADeleteClause.class);
        when(queryFactory.delete(any(EntityPath.class))).thenReturn(clause);
        when(clause.where(any(Predicate.class))).thenReturn(clause);
        when(clause.execute()).thenReturn(1L);

        service.deleteUserByIds(List.of(1L));

        verify(queryFactory).delete(any(EntityPath.class));
    }

    @Test
    @DisplayName("findByUserNames：空入参返回空集合，否则走 QueryDSL")
    void findByUserNames() {
        assertThat(service.findByUserNames(null)).isEmpty();
        assertThat(service.findByUserNames(new ArrayList<>())).isEmpty();

        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.fetch()).thenReturn(List.of(activeUser(1L)));

        assertThat(service.findByUserNames(List.of("user1"))).hasSize(1);
    }

    @Test
    @DisplayName("updateUserRoles：用户存在时覆盖角色")
    void updateUserRoles() {
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Role role = new Role();
        role.setId(5L);
        service.updateUserRoles(1L, List.of(role));

        assertThat(user.getUserRoles()).hasSize(1);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateUserInfoBySelf / updateInfoBySelf：按当前登录人更新资料")
    void updateSelf() {
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        LoginUserIdContextHolder.setUserId(1L);

        UpdateUserInfo info = new UpdateUserInfo();
        info.setName("self");
        info.setEmailAddress("a@b.c");
        service.updateUserInfoBySelf(info);

        assertThat(user.getName()).isEqualTo("self");
        verify(userRepository).save(user);

        UserInfoDto dto = new UserInfoDto();
        dto.setName("self2");
        service.updateInfoBySelf(dto);
        assertThat(user.getName()).isEqualTo("self2");
    }

    // ------------------------------------------------------------------
    // 创建 / 编辑用户
    // ------------------------------------------------------------------

    @Test
    @DisplayName("createOrUpdateUser：无 id 走创建，有 id 走更新")
    void createOrUpdateUserBranches() {
        CreateOrUpdateUserInput createInput = new CreateOrUpdateUserInput();
        UserEditDto createDto = new UserEditDto();
        createDto.setName("n");
        createDto.setPassword("p");
        createInput.setUser(createDto);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(101L);
            return u;
        });

        assertThat(service.createOrUpdateUser(createInput)).isEqualTo(101L);

        User existing = activeUser(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
        CreateOrUpdateUserInput updateInput = new CreateOrUpdateUserInput();
        UserEditDto updateDto = new UserEditDto();
        updateDto.setId(5L);
        updateDto.setName("n2");
        updateDto.setPassword("p2");
        updateInput.setUser(updateDto);

        assertThat(service.createOrUpdateUser(updateInput)).isEqualTo(5L);
    }

    @Test
    @DisplayName("createUser：密码为空时抛业务异常")
    void createUserThrowsWhenPasswordBlank() {
        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        input.setUser(new UserEditDto());

        assertThatThrownBy(() -> service.createUser(input, EUnitType.Inner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不能为空");
    }

    @Test
    @DisplayName("createUser：租户用户数超限时抛业务异常")
    void createUserThrowsWhenMaxUsersReached() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.isEnabled(UserFeatureProvider.APP_USER_MAX_USERS)).thenReturn(true);
        when(featureChecker.getValue(UserFeatureProvider.APP_USER_MAX_USERS_NUMBER)).thenReturn("0");
        when(userRepository.count()).thenReturn(1L);

        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setPassword("p");
        input.setUser(dto);

        assertThatThrownBy(() -> service.createUser(input, EUnitType.Inner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户数超出当前限制");
    }

    @BeforeEach
    void setUp() {
        service = spy(new UserServiceImpl());
        ReflectionTestUtils.setField(service, "repository", userRepository);
        ReflectionTestUtils.setField(service, "em", entityManager);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "grantPermissionRepository", grantPermissionRepository);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "roleService", roleService);
        ReflectionTestUtils.setField(service, "organizationUnitService", organizationUnitService);
        ReflectionTestUtils.setField(service, "featureChecker", featureChecker);
        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "tenantRepository", tenantRepository);
        ReflectionTestUtils.setField(service, "appAuthConfig", appAuthConfig);
        ReflectionTestUtils.setField(service, "smsPushConfig", smsPushConfig);
        ReflectionTestUtils.setField(service, "pushRpcService", pushRpcService);
        ReflectionTestUtils.setField(service, "emailRpcService", emailRpcService);
        ReflectionTestUtils.setField(service, "tokenAuthManager", tokenAuthManager);
        ReflectionTestUtils.setField(service, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(service, "organizationManagerRepository", organizationManagerRepository);
        ReflectionTestUtils.setField(service, "stationService", stationService);
        ReflectionTestUtils.setField(service, "settingRpcService", settingRpcService);
        ReflectionTestUtils.setField(service, "sender", sender);
        ReflectionTestUtils.setField(service, "passwdLen", 8);
        ReflectionTestUtils.setField(service, "activeAddr", "http://active");
        ReflectionTestUtils.setField(service, "authzSubjectVersionService", authzSubjectVersionService);
        ReflectionTestUtils.setField(service, "authzEventPublisher", authzEventPublisher);
        when(appAuthConfig.getLoginEncryptKey()).thenReturn(null);
    }

    @Test
    @DisplayName("createUser：随机密码模式发送邮件并创建用户")
    void createUserWithRandomPassword() {
        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("张三");
        dto.setPassword("p");
        dto.setEmailAddress("a@b.c");
        input.setUser(dto);
        input.setSetRandomPassword(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(102L);
            return u;
        });

        assertThat(service.createUser(input, EUnitType.Inner)).isEqualTo(102L);
        verify(emailRpcService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("createOrUpdateUserExistByUserName：两个重载分别覆盖创建与更新分支")
    void createOrUpdateUserExistByUserName() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(103L);
            return u;
        });
        when(userRepository.findById(103L)).thenReturn(Optional.of(activeUser(103L)));

        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("n");
        dto.setPassword("p");
        dto.setUserName("u");
        input.setUser(dto);
        service.createOrUpdateUserExistByUserName(input);
        verify(userRepository, times(1)).save(any(User.class));

        CreateOrUpdateUserInfoInput infoInput = new CreateOrUpdateUserInfoInput();
        UserEditDto infoDto = new UserEditDto();
        infoDto.setName("n");
        infoDto.setPassword("p");
        infoInput.setUser(infoDto);
        infoInput.setLevel(ELevel.General);
        service.createOrUpdateUserExistByUserName(infoInput);
        verify(userRepository, times(3)).save(any(User.class));
    }

    @Test
    @DisplayName("createExternalUser：账号信息为空时直接保存；超限时抛异常")
    void createExternalUser() {
        CreateExternalUserInput input = new CreateExternalUserInput();
        input.setName("ext");
        input.setLevel(ELevel.General);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(104L);
            return u;
        });

        assertThat(service.createExternalUser(input)).isEqualTo(104L);

        TenantContextHolder.setTenantId(1L);
        when(featureChecker.isEnabled(UserFeatureProvider.APP_USER_MAX_USERS)).thenReturn(true);
        when(featureChecker.getValue(UserFeatureProvider.APP_USER_MAX_USERS_NUMBER)).thenReturn("0");
        when(userRepository.count()).thenReturn(5L);

        assertThatThrownBy(() -> service.createExternalUser(new CreateExternalUserInput()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户数超出当前限制");
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("createExternalUser：携带账号信息时走 saveUserAccount")
    void createExternalUserWithAccount() {
        CreateExternalUserInput input = new CreateExternalUserInput();
        input.setName("ext");
        ExternalUserSettingDto dto = new ExternalUserSettingDto();
        dto.setUserName("extuser");
        dto.setPassword("raw");
        input.setDto(dto);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(105L);
            return u;
        });

        assertThat(service.createExternalUser(input)).isEqualTo(105L);
    }

    @Test
    @DisplayName("updateExternalUserInfo：按 id 查找后保存账号信息")
    void updateExternalUserInfo() {
        User user = activeUser(1L);
        user.setUserName("u1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ExternalUserSettingDto dto = new ExternalUserSettingDto();
        dto.setId(1L);
        dto.setUserName("u1");

        assertThat(service.updateExternalUserInfo(dto)).isEqualTo(1L);
    }

    @Test
    @DisplayName("getUserForEdit：新建用户返回默认用户信息")
    void getUserForEditForNewUser() {
        when(roleService.findAll()).thenReturn(new ArrayList<>());

        GetUserForEditOutput output = service.getUserForEdit(new EntityDto());

        assertThat(output.getUser().isActive()).isTrue();
        assertThat(output.getUser().isShouldChangePasswordOnNextLogin()).isTrue();
    }

    @Test
    @DisplayName("getUserForEdit：已有用户回填角色、组织与级别")
    void getUserForEditForExistingUser() {
        when(roleService.findAll()).thenReturn(new ArrayList<>());
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(stationService.getStationsForFrontByUserId(1L)).thenReturn(new ArrayList<>());
        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(List.of(orgUnit(1L, null)));
        stubManagerQuery(new ArrayList<>());

        GetUserForEditOutput output = service.getUserForEdit(new EntityDto(1L));

        assertThat(output.getLevel()).isEqualTo(ELevel.General);
        assertThat(output.getMemberedOrganizationUnits()).containsExactly(1L);
    }

    @Test
    @DisplayName("getUserForEdit：存在管理组织时级别为管理层")
    void getUserForEditWithManagerOrg() {
        when(roleService.findAll()).thenReturn(new ArrayList<>());
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(stationService.getStationsForFrontByUserId(1L)).thenReturn(new ArrayList<>());
        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(new ArrayList<>());
        stubManagerQuery(List.of(orgUnit(3L, null)));

        GetUserForEditOutput output = service.getUserForEdit(new EntityDto(1L));

        assertThat(output.getLevel()).isEqualTo(ELevel.Manager);
        assertThat(output.getManagerOrgDtos()).hasSize(1);
    }

    @Test
    @DisplayName("getExternalUserEditInfo：回填角色、组织与管理组织")
    void getExternalUserEditInfo() {
        when(roleService.findAll()).thenReturn(new ArrayList<>());
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(new ArrayList<>());
        stubManagerQuery(new ArrayList<>());

        assertThat(service.getExternalUserEditInfo(1L)).isNotNull();
    }

    @Test
    @DisplayName("getUserInfo：组装基础信息、账号信息与上级")
    void getUserInfo() {
        when(roleService.findAll()).thenReturn(new ArrayList<>());
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(new ArrayList<>());
        stubManagerQuery(new ArrayList<>());

        GetUserInfoOutput output = service.getUserInfo(1L);

        assertThat(output.getBaseUserInfoDto()).isNotNull();
        assertThat(output.getAccountInfoDto()).isNotNull();
        assertThat(output.getBaseUserInfoDto().getLevel()).isEqualTo(ELevel.General);
        assertThat(output.getBaseUserInfoDto().getSuperior()).isNull();
    }

    @Test
    @DisplayName("getUsersToExcel：按组织路径汇总后映射导出对象")
    void getUsersToExcel() {
        User user = activeUser(1L);
        user.setUserType(EUnitType.Inner);
        when(userRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(user));

        OrganizationUnit root = orgUnit(1L, null);
        OrganizationUnit child = orgUnit(2L, 1L);
        when(organizationUnitService.findAll()).thenReturn(List.of(root, child, child));
        OrganizationUnitUserListDto orgUser = new OrganizationUnitUserListDto();
        orgUser.setId(1L);
        orgUser.setOrganizationUnitId(2L);
        when(organizationUnitService.getOrganizationUnitUsers(any(GetOrganizationUnitUsersInput.class)))
                .thenReturn(new PageImpl<>(List.of(orgUser)));

        service.getUsersToExcel(null);

        verify(userRepository).findAll(any(Specification.class), any(Sort.class));
    }

    // ------------------------------------------------------------------
    // 找回密码
    // ------------------------------------------------------------------

    @Test
    @DisplayName("createUser：邮箱发送失败时包装为业务异常")
    void createUserThrowsWhenMailFails() {
        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("n");
        dto.setPassword("p");
        dto.setEmailAddress("mail@dusk.com");
        input.setUser(dto);
        input.setSetRandomPassword(true);
        doThrow(new RuntimeException("smtp down"))
                .when(emailRpcService).sendEmail(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.createUser(input, EUnitType.Inner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮件发送失败");
    }

    @Test
    @DisplayName("getForgetPwdCaptchaByMobile：租户策略不允许/手机号不匹配均抛异常")
    void getForgetPwdCaptchaByMobileGuards() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.getValue(com.dusk.module.auth.feature.LoginFeatureProvider.APP_LOGIN_FORGET_PWD))
                .thenReturn("email");
        assertThatThrownBy(() -> service.getForgetPwdCaptchaByMobile("u", "13800000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许通过手机找回密码");
        TenantContextHolder.clear();

        User user = activeUser(1L);
        user.setPhoneNo("13900000000");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.getForgetPwdCaptchaByMobile("user1", "13800000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号不匹配");
    }

    @Test
    @DisplayName("getForgetPwdCaptchaByMobile：发送频率过快时抛异常")
    void getForgetPwdCaptchaByMobileThrottles() {
        User user = activeUser(1L);
        user.setPhoneNo("13900000000");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));
        when(redisUtil.getCache("CRUX:PWD:RESET:SENDHOST:user1")).thenReturn("1");

        assertThatThrownBy(() -> service.getForgetPwdCaptchaByMobile("user1", "13900000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("发送频率过快");
    }

    @Test
    @DisplayName("getForgetPwdCaptchaByEmail：邮箱不匹配与租户策略不允许抛异常")
    void getForgetPwdCaptchaByEmailGuards() {
        User user = activeUser(1L);
        user.setEmailAddress("a@b.c");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.getForgetPwdCaptchaByEmail("user1", "x@y.z"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱地址不匹配");

        TenantContextHolder.setTenantId(1L);
        when(featureChecker.getValue(com.dusk.module.auth.feature.LoginFeatureProvider.APP_LOGIN_FORGET_PWD))
                .thenReturn("mobile");
        assertThatThrownBy(() -> service.getForgetPwdCaptchaByEmail("user1", "a@b.c"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许通过邮箱找回密码");
    }

    @Test
    @DisplayName("getForgetPwdCaptchaByMobile：正常发送验证码并写入缓存")
    void getForgetPwdCaptchaByMobileSends() {
        User user = activeUser(1L);
        user.setPhoneNo("13900000000");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));

        service.getForgetPwdCaptchaByMobile("user1", "13900000000");

        verify(pushRpcService).smsPushAsync(any());
        verify(redisUtil, times(2)).setCache(anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("resetPwdWithCaptcha：验证码正确时重置密码并清理缓存，错误时抛异常")
    void resetPwdWithCaptcha() {
        User user = activeUser(1L);
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("np")).thenReturn("NEWHASH");

        when(redisUtil.getCache("CRUX:PWD:RESET:host:user1")).thenReturn("123456");
        assertThatThrownBy(() -> service.resetPwdWithCaptcha("user1", "np", "000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码错误");

        service.resetPwdWithCaptcha("user1", "np", "123456");

        assertThat(user.getPassword()).isEqualTo("NEWHASH");
        assertThat(user.isShouldChangePasswordOnNextLogin()).isFalse();
        verify(redisUtil, times(2)).deleteCache(anyString());
    }

    // ------------------------------------------------------------------
    // Token / 其他
    // ------------------------------------------------------------------

    @Test
    @DisplayName("generateTokenByUserName / generateExpireTokenByUserName：生成令牌")
    void generateTokenByUserName() {
        User user = activeUser(1L);
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));
        when(tokenAuthManager.generateToken(any(UserContext.class))).thenReturn("T1");

        assertThat(service.generateTokenByUserName("user1")).isEqualTo("T1");

        when(tokenAuthManager.generateToken(any(UserContext.class), anyLong(), any()))
                .thenReturn("T2");
        assertThat(service.generateExpireTokenByUserName("user1", 5L, java.util.concurrent.TimeUnit.MINUTES))
                .isEqualTo("T2");
    }

    @Test
    @DisplayName("generateTokenByUserName：账户不存在时抛业务异常")
    void generateTokenByUserNameThrowsWhenMissing() {
        when(userRepository.findByUserName("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateTokenByUserName("none"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法找到账户名");
    }

    @Test
    @DisplayName("changeAdminPasswordByHost：未找到租户管理员抛异常，找到则重置密码")
    void changeAdminPasswordByHost() {
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeAdminPasswordByHost(1L, "np"))
                .isInstanceOf(BusinessException.class);

        User admin = activeUser(1L);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("np")).thenReturn("H");

        service.changeAdminPasswordByHost(1L, "np");

        assertThat(admin.getPassword()).isEqualTo("H");
        verify(userRepository).saveAndFlush(admin);
    }

    @Test
    @DisplayName("decryptSm4：密钥不可用时原样返回入参")
    void decryptSm4FallsBackToInput() {
        assertThat(service.decryptSm4("plain", "err")).isEqualTo("plain");
    }

    @Test
    @DisplayName("getUsersByOrg 由 UserRpcServiceImpl 覆盖；此处仅校验 LoginUtils 契约")
    void loginUtilsBuildsContext() {
        User user = activeUser(1L);
        Role role = new Role();
        role.setId(5L);
        user.getUserRoles().add(role);

        UserContext context = LoginUtils.getUserContextByUser(user);

        assertThat(context.getId()).isEqualTo(1L);
        assertThat(context.getAuthorities()).hasSize(1);
    }

    // ==================================================================
    // 补充：登录解密、查询 Specification、角色回填、资料校验、私有分支
    // ==================================================================

    @Test
    @DisplayName("getForgetPwdCaptchaByEmail：正常发送验证码")
    void getForgetPwdCaptchaByEmailSends() {
        User user = activeUser(1L);
        user.setEmailAddress("a@b.c");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));

        service.getForgetPwdCaptchaByEmail("user1", "a@b.c");

        verify(emailRpcService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("checkAndGetUser：合法 SM4 密文可成功解密")
    void checkAndGetUserDecryptsSm4() {
        String key = "0123456789abcdeffedcba9876543210";
        when(appAuthConfig.getLoginEncryptKey()).thenReturn(key);
        String encrypted = SmUtil.sm4(HexUtil.decodeHex(key)).encryptHex("pwd");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(activeUser(1L)));

        assertThat(service.checkAndGetUser("user1", encrypted)).isNotNull();
    }

    @Test
    @DisplayName("checkAndGetUser：租户存在但忽略大小写特性关闭时按原样查询")
    void checkAndGetUserUsesExactNameWhenFeatureDisabled() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.isEnabled(com.dusk.module.auth.feature.LoginFeatureProvider.APP_LOGIN_IGNORE_CASE))
                .thenReturn(false);
        when(userRepository.findByUserName("User1")).thenReturn(Optional.of(activeUser(1L)));

        assertThat(service.checkAndGetUser("User1", "pwd")).isNotNull();
        verify(userRepository).findByUserName("User1");
    }

    @Test
    @DisplayName("getUsers：全部过滤条件的 Specification 均可执行")
    @SuppressWarnings("unchecked")
    void getUsersEvaluatesSpecification() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeUser(1L))));
        JPAQuery roleQuery = queryMock();
        when(queryFactory.select(any(Expression.class))).thenReturn(roleQuery);
        when(roleQuery.fetch()).thenReturn(List.of(10L), List.of(10L), List.of(10L), List.of());

        service.getUsers(new GetUsersInput());

        GetUsersInput full = new GetUsersInput();
        full.setFilter("na");
        full.setRoleId(7L);
        full.setRoleCode("C");
        full.setRoleName("R");
        full.setPermission("P");
        full.setUserType(EUnitType.Inner);
        full.setDisplayDimissionUsers(false);
        full.setOnlyLockedUsers(true);
        service.getUsers(full);

        GetUsersInput emptyRoleFetch = new GetUsersInput();
        emptyRoleFetch.setFilter("na");
        emptyRoleFetch.setUserType(null);
        emptyRoleFetch.setDisplayDimissionUsers(true);
        emptyRoleFetch.setOnlyLockedUsers(false);
        service.getUsers(emptyRoleFetch);

        ArgumentCaptor<Specification<User>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(userRepository, times(3)).findAll(captor.capture(), any(Pageable.class));
        captor.getAllValues().forEach(UserServiceImplTest::evaluateSpecification);
    }

    @Test
    @DisplayName("getUsersList：锁定时间已过时不标记锁定")
    void getUsersListSkipsExpiredLock() {
        User expired = activeUser(1L);
        expired.setLockoutEndDateUtc(LocalDateTime.now().minusHours(1));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(expired)));

        assertThat(service.getUsersList(new GetUsersInput()).getItems().getFirst().isLock()).isFalse();
    }

    @Test
    @DisplayName("getUsersList：未锁定用户的锁定标记为 false")
    void getUsersListHandlesNullLockDate() {
        User unlocked = activeUser(2L);
        unlocked.setLockoutEndDateUtc(null);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unlocked)));

        assertThat(service.getUsersList(new GetUsersInput()).getItems().getFirst().isLock()).isFalse();
    }

    @Test
    @DisplayName("getUsersForLogin：三个模糊条件的 Specification 可执行")
    @SuppressWarnings("unchecked")
    void getUsersForLoginEvaluatesSpecification() {
        when(userRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(activeUser(1L)));

        service.getUsersForLogin(new GetUsersForLoginInput());

        GetUsersForLoginInput input = new GetUsersForLoginInput();
        input.setName("na");
        input.setSurName("su");
        input.setUserName("user");
        service.getUsersForLogin(input);

        ArgumentCaptor<Specification<User>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(userRepository, times(2)).findAll(captor.capture(), any(Sort.class));
        captor.getAllValues().forEach(UserServiceImplTest::evaluateSpecification);
    }

    @Test
    @DisplayName("getOrgaUsers：无角色过滤条件时角色为空")
    void getOrgaUsersWithoutRoleFilters() {
        when(organizationUnitService.findById(1L)).thenReturn(Optional.of(orgUnit(1L, null)));
        JPAQuery query = queryMock();
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        Page<User> page = new PageImpl<>(List.of(activeUser(1L)));
        doReturn(page).when(service).page(any(), any());

        GetOrgaUsersInput input = new GetOrgaUsersInput();
        input.setOrgaId(1L);

        assertThat(service.getOrgaUsers(input)).isSameAs(page);
    }

    @Test
    @DisplayName("getUsersByRoleCodes：allRoles=true 且 userType 为空")
    void getUsersByRoleCodesAllRoles() {
        JPAQuery roleQuery = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(roleQuery);
        when(roleQuery.fetch()).thenReturn(List.of(10L));
        JPAQuery userQuery = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(userQuery);
        Page<User> page = new PageImpl<>(List.of(activeUser(1L)));
        doReturn(page).when(service).page(any(), any());

        GetUsersByRoleCodesInput input = new GetUsersByRoleCodesInput();
        input.setRoleCodeList(List.of("C1"));
        input.setAllRoles(true);
        input.setUserType(null);

        assertThat(service.getUsersByRoleCodes(input)).isSameAs(page);
    }

    @Test
    @DisplayName("getUsersByRoleName：userType 为空时跳过滤条件")
    void getUsersByRoleNameWithoutUserType() {
        JPAQuery roleQuery = queryMock();
        when(queryFactory.select(any(Expression.class))).thenReturn(roleQuery);
        when(roleQuery.fetch()).thenReturn(List.of(1L));
        JPAQuery userQuery = mock(JPAQuery.class);
        when(queryFactory.selectDistinct(any(Expression.class))).thenReturn(userQuery);
        when(userQuery.from(any(EntityPath.class))).thenReturn(userQuery);
        when(userQuery.where(any(Predicate.class))).thenReturn(userQuery);
        Page<User> page = new PageImpl<>(List.of(activeUser(1L)));
        doReturn(page).when(service).page(any(), any());

        GetUsersByRoleNameInput input = new GetUsersByRoleNameInput();
        input.setRoleNames(List.of("adm"));
        input.setUserType(null);

        assertThat(service.getUsersByRoleName(input)).isSameAs(page);
    }

    @Test
    @DisplayName("getUsersToExcel：无组织映射与空组织列表的用户不写组织路径")
    void getUsersToExcelHandlesMissingOrgMappings() {
        List<User> users = new ArrayList<>();
        for (long id = 1; id <= 4; id++) {
            User user = activeUser(id);
            user.setUserType(EUnitType.Inner);
            users.add(user);
        }
        when(userRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(users);

        OrganizationUnit root = orgUnit(1L, null);
        OrganizationUnit child = orgUnit(2L, 1L);
        when(organizationUnitService.findAll()).thenReturn(List.of(root, child));

        OrganizationUnitUserListDto nullOrgId = new OrganizationUnitUserListDto();
        nullOrgId.setId(1L);
        OrganizationUnitUserListDto unknownOrgId = new OrganizationUnitUserListDto();
        unknownOrgId.setId(2L);
        unknownOrgId.setOrganizationUnitId(99L);
        OrganizationUnitUserListDto mapped = new OrganizationUnitUserListDto();
        mapped.setId(3L);
        mapped.setOrganizationUnitId(2L);
        when(organizationUnitService.getOrganizationUnitUsers(any(GetOrganizationUnitUsersInput.class)))
                .thenReturn(new PageImpl<>(List.of(nullOrgId, unknownOrgId, mapped)));

        service.getUsersToExcel(null);

        verify(userRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    @DisplayName("getUserForEdit / getExternalUserEditInfo / getUserInfo：角色回填与上级、管理组织")
    void roleAssignmentAndManagerInfo() {
        RoleMapper roleMapper = mock(RoleMapper.class);
        ReflectionTestUtils.setField(service, "roleMapper", roleMapper);
        Role role = new Role();
        role.setId(5L);
        Role otherRole = new Role();
        otherRole.setId(6L);
        UserRoleDto roleDto = new UserRoleDto();
        roleDto.setRoleId(5L);
        UserRoleDto otherRoleDto = new UserRoleDto();
        otherRoleDto.setRoleId(6L);
        when(roleService.findAll()).thenReturn(List.of(role, otherRole));
        when(roleMapper.toRoleDto(role)).thenReturn(roleDto);
        when(roleMapper.toRoleDto(otherRole)).thenReturn(otherRoleDto);

        User user = activeUser(1L);
        user.getUserRoles().add(role);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(stationService.getStationsForFrontByUserId(1L)).thenReturn(new ArrayList<>());
        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(List.of(orgUnit(1L, null)));
        OrganizationManager manager = new OrganizationManager();
        manager.setUserId(7L);
        when(organizationManagerRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.of(manager));
        when(userRepository.findById(7L)).thenReturn(Optional.of(activeUser(7L)));
        stubManagerQuery(List.of(orgUnit(3L, null)));

        GetUserForEditOutput editOutput = service.getUserForEdit(new EntityDto(1L));
        assertThat(editOutput.getRoles().getFirst().isAssigned()).isTrue();

        GetExternalUserEditOutput externalOutput = service.getExternalUserEditInfo(1L);
        assertThat(externalOutput.getRoles().getFirst().isAssigned()).isTrue();

        GetUserInfoOutput infoOutput = service.getUserInfo(1L);
        assertThat(infoOutput.getBaseUserInfoDto().getLevel()).isEqualTo(ELevel.Manager);
        assertThat(infoOutput.getBaseUserInfoDto().getManagerDtos()).hasSize(1);
        assertThat(infoOutput.getBaseUserInfoDto().getSuperior()).isEqualTo("user7");
        assertThat(infoOutput.getAccountInfoDto().getRoles().getFirst().isAssigned()).isTrue();
    }

    @Test
    @DisplayName("getUserInfo：无管理组织且无上级时保持普通级别")
    void getUserInfoWithoutManagerOrSuperior() {
        when(roleService.findAll()).thenReturn(new ArrayList<>());
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(organizationUnitService.getOrganizationUnitsByUser(any(EntityDto.class)))
                .thenReturn(new ArrayList<>());
        stubManagerQuery(new ArrayList<>());

        GetUserInfoOutput output = service.getUserInfo(1L);

        assertThat(output.getBaseUserInfoDto().getLevel()).isEqualTo(ELevel.General);
        assertThat(output.getBaseUserInfoDto().getManagerDtos()).isNull();
        assertThat(output.getBaseUserInfoDto().getSuperior()).isNull();
    }

    @Test
    @DisplayName("updatePersonalInfo：工号唯一且管理组织为空时只更新组织关系")
    void updatePersonalInfoWithUniqueWorkNumberAndNoManagers() {
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        OrganizationUnit station = orgUnit(10L, null);
        station.setStation(true);
        when(organizationUnitService.findAllById(List.of(10L))).thenReturn(List.of(station));

        PersonalInfoInput input = new PersonalInfoInput();
        input.setId(1L);
        input.setName("newName");
        input.setWorkNumber("w-new");
        input.setOrgaId(10L);
        input.setManagerOrgIds(null);

        service.updatePersonalInfo(input);

        assertThat(user.getDefaultStation()).isEqualTo(10L);
        assertThat(user.getOrganizationUnit()).containsExactly(station);
        verify(organizationManagerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("updatePersonalInfo：工号重复时抛业务异常")
    void updatePersonalInfoThrowsOnDuplicateWorkNumber() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser(1L)));
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(activeUser(9L)));

        PersonalInfoInput input = new PersonalInfoInput();
        input.setId(1L);
        input.setName("n");
        input.setWorkNumber("dup");

        assertThatThrownBy(() -> service.updatePersonalInfo(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工号已被占用");
    }

    @Test
    @DisplayName("createOrUpdateUserExistByUserName：两个重载的更新分支")
    void createOrUpdateUserExistByUserNameUpdates() {
        User user = activeUser(5L);
        user.setName("n5");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        JPAQuery managerQuery = queryMock();
        when(queryFactory.select(any(Expression.class))).thenReturn(managerQuery);
        when(managerQuery.fetch()).thenReturn(new ArrayList<>());

        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setId(5L);
        dto.setName("n5");
        dto.setUserName("u5");
        dto.setPassword("p");
        input.setUser(dto);
        service.createOrUpdateUserExistByUserName(input);

        CreateOrUpdateUserInfoInput infoInput = new CreateOrUpdateUserInfoInput();
        UserEditDto infoDto = new UserEditDto();
        infoDto.setId(5L);
        infoDto.setName("n5");
        infoDto.setUserName("u5");
        infoDto.setPassword("p");
        infoInput.setUser(infoDto);
        infoInput.setLevel(ELevel.Manager);
        infoInput.setManagerOrgIds(List.of(10L));
        infoInput.setStationIds(new ArrayList<>());
        service.createOrUpdateUserExistByUserName(infoInput);

        verify(organizationManagerRepository).saveAll(any());
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    @DisplayName("createOrUpdateUserExistByUserName：管理层但未指定管理组织时只清空旧关系")
    void createOrUpdateUserExistByUserNameManagerWithoutOrgs() {
        User user = activeUser(5L);
        user.setName("n5");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        CreateOrUpdateUserInfoInput infoInput = new CreateOrUpdateUserInfoInput();
        UserEditDto infoDto = new UserEditDto();
        infoDto.setId(5L);
        infoDto.setName("n5");
        infoDto.setUserName("u5");
        infoDto.setPassword("p");
        infoInput.setUser(infoDto);
        infoInput.setLevel(ELevel.Manager);
        infoInput.setManagerOrgIds(new ArrayList<>());
        infoInput.setStationIds(new ArrayList<>());

        service.createOrUpdateUserExistByUserName(infoInput);

        verify(organizationManagerRepository).deleteByUserId(5L);
        verify(organizationManagerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("createOrUpdateUserExistByUserName：管理组织已存在管理人员时抛异常")
    void createOrUpdateUserExistByUserNameThrowsWhenManagerExists() {
        User user = activeUser(5L);
        user.setName("n5");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        stubManagerQuery(List.of(orgUnit(10L, null)));

        CreateOrUpdateUserInfoInput infoInput = new CreateOrUpdateUserInfoInput();
        UserEditDto infoDto = new UserEditDto();
        infoDto.setId(5L);
        infoDto.setName("n5");
        infoDto.setUserName("u5");
        infoDto.setPassword("p");
        infoInput.setUser(infoDto);
        infoInput.setLevel(ELevel.Manager);
        infoInput.setManagerOrgIds(List.of(10L));

        assertThatThrownBy(() -> service.createOrUpdateUserExistByUserName(infoInput))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经存在管理人员");
    }

    @Test
    @DisplayName("createExternalUser：工号重复时抛业务异常")
    void createExternalUserThrowsOnDuplicateWorkNumber() {
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(activeUser(9L)));

        CreateExternalUserInput input = new CreateExternalUserInput();
        input.setName("ext");
        input.setWorkNumber("w-dup");

        assertThatThrownBy(() -> service.createExternalUser(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工号已被占用");
    }

    @Test
    @DisplayName("createExternalUser：租户用户上限配置为空时 parseInt 抛 NumberFormatException（生产缺陷）")
    void createExternalUserWithNullMaxUsersFailsFast() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.isEnabled(UserFeatureProvider.APP_USER_MAX_USERS)).thenReturn(true);
        when(featureChecker.getValue(UserFeatureProvider.APP_USER_MAX_USERS_NUMBER)).thenReturn(null);
        when(userRepository.count()).thenReturn(1L);

        CreateExternalUserInput input = new CreateExternalUserInput();
        input.setName("ext");

        assertThatThrownBy(() -> service.createExternalUser(input))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("createExternalUser：租户用户上限特性关闭时正常创建并校验唯一工号")
    void createExternalUserWithFeatureDisabled() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.isEnabled(UserFeatureProvider.APP_USER_MAX_USERS)).thenReturn(false);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(401L);
            return u;
        });

        CreateExternalUserInput input = new CreateExternalUserInput();
        input.setName("ext");
        input.setWorkNumber("w-unique");

        assertThat(service.createExternalUser(input)).isEqualTo(401L);
    }

    @Test
    @DisplayName("createExternalUser：用户数未达上限时允许创建")
    void createExternalUserUnderMaxUsers() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.isEnabled(UserFeatureProvider.APP_USER_MAX_USERS)).thenReturn(true);
        when(featureChecker.getValue(UserFeatureProvider.APP_USER_MAX_USERS_NUMBER)).thenReturn("10");
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(402L);
            return u;
        });

        CreateExternalUserInput input = new CreateExternalUserInput();
        input.setName("ext");

        assertThat(service.createExternalUser(input)).isEqualTo(402L);
    }

    @Test
    @DisplayName("saveUserAccount：已有用户但账号为空时重置密码并同步厂站与角色")
    void saveUserAccountResetsPasswordAndStations() {
        User user = activeUser(1L);
        user.setName("张三");
        user.setUserName("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pwd")).thenReturn("H");
        Role role = new Role();
        role.setId(5L);
        when(roleService.findAllById(List.of(5L))).thenReturn(List.of(role));
        StationsOfLoginUserDto old1 = new StationsOfLoginUserDto();
        old1.setValue(1L);
        StationsOfLoginUserDto old2 = new StationsOfLoginUserDto();
        old2.setValue(2L);
        when(stationService.getStationsForFrontByUserId(1L)).thenReturn(List.of(old1, old2));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ExternalUserSettingDto dto = new ExternalUserSettingDto();
        dto.setId(1L);
        dto.setUserName("u1");
        dto.setPassword("pwd");
        dto.setStations(new ArrayList<>(List.of(2L, 3L)));
        dto.setAssignedRoleIds(List.of(5L));

        assertThat(service.updateExternalUserInfo(dto)).isEqualTo(1L);

        assertThat(user.getPassword()).isEqualTo("H");
        assertThat(user.getUserRoles()).containsExactly(role);
        verify(stationService).removeUserFromStation(org.mockito.ArgumentMatchers.argThat(
                r -> r.getStationId().equals(1L)));
        verify(stationService).addUsersToStation(org.mockito.ArgumentMatchers.argThat(
                a -> a.getStationId().equals(3L)));
    }

    @Test
    @DisplayName("saveUserAccount：账号重复时抛业务异常")
    void saveUserAccountThrowsOnDuplicateUserName() {
        User user = activeUser(1L);
        user.setUserName("u1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(activeUser(9L)));

        ExternalUserSettingDto dto = new ExternalUserSettingDto();
        dto.setId(1L);
        dto.setUserName("dup");

        assertThatThrownBy(() -> service.updateExternalUserInfo(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已被占用");
    }

    @Test
    @DisplayName("deleteUsers：null 入参抛异常，登录用户自身被排除")
    void deleteUsersHandlesNullAndExcludesLoginUser() {
        assertThatThrownBy(() -> service.deleteUsers(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");

        LoginUserIdContextHolder.setUserId(1L);
        when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(activeUser(2L)));

        service.deleteUsers(new ArrayList<>(List.of(new EntityDto(1L), new EntityDto(2L))));

        verify(userRepository).deleteByIdIn(List.of(2L));
        verify(organizationManagerRepository).deleteByUserIdIn(List.of(2L));
    }

    @Test
    @DisplayName("deleteUsers：用户 id 为空时跳过代际推进")
    void deleteUsersSkipsSubjectChangeForTransientUser() {
        LoginUserIdContextHolder.setUserId(99L);
        User transientUser = activeUser(null);
        when(userRepository.findAllById(List.of(3L))).thenReturn(List.of(transientUser));

        service.deleteUsers(new ArrayList<>(List.of(new EntityDto(3L))));

        verify(authzSubjectVersionService, never()).nextVersion(anyString(), any());
        verify(userRepository).deleteByIdIn(List.of(3L));
    }

    @Test
    @DisplayName("changeStatus：用户 id 为空时跳过代际推进；活跃状态组合分别计算 enabled")
    void changeStatusPublishesSubjectChangedBranches() {
        User transientUser = activeUser(null);
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(transientUser));
        ChangeStatusInput input = new ChangeStatusInput();
        input.setUserIds(List.of(1L));
        input.setStatus(UserStatus.OnJob);
        service.changeStatus(input);
        verify(authzSubjectVersionService, never()).nextVersion(anyString(), any());

        User inactive = activeUser(2L);
        inactive.setActive(false);
        inactive.setUserStatus(null);
        when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(inactive));
        ChangeStatusInput second = new ChangeStatusInput();
        second.setUserIds(List.of(2L));
        second.setStatus(UserStatus.OnJob);
        service.changeStatus(second);

        User dimission = activeUser(3L);
        dimission.setUserStatus(UserStatus.Dimission);
        when(userRepository.findAllById(List.of(3L))).thenReturn(List.of(dimission));
        ChangeStatusInput third = new ChangeStatusInput();
        third.setUserIds(List.of(3L));
        third.setStatus(UserStatus.Dimission);
        service.changeStatus(third);

        verify(authzSubjectVersionService, times(2)).nextVersion(anyString(), any());
        verify(sender).sendAsync(any());
    }

    @Test
    @DisplayName("publishSubjectChanged：active 且 status=null 时 enabled=true")
    void publishSubjectChangedWithNullStatus() {
        User user = activeUser(1L);
        user.setUserStatus(null);

        ReflectionTestUtils.invokeMethod(service, "publishSubjectChanged", user);

        verify(authzSubjectVersionService).nextVersion(anyString(), org.mockito.ArgumentMatchers.eq(1L));
    }

    @Test
    @DisplayName("publishSubjectChanged：null 用户直接返回")
    void publishSubjectChangedIgnoresNullUser() {
        ReflectionTestUtils.invokeMethod(service, "publishSubjectChanged", (User) null);
        ReflectionTestUtils.invokeMethod(service, "publishSubjectChanged", (User) null, false);

        verify(authzSubjectVersionService, never()).nextVersion(anyString(), any());
        verify(authzEventPublisher, never()).subjectChanged(anyString(), any(), org.mockito.ArgumentMatchers.anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("activeUser：redisUtil 为空或缓存未命中时正常激活")
    void activeUserHandlesRedisAbsent() {
        ReflectionTestUtils.setField(service, "redisUtil", null);
        User user = activeUser(1L);
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.activeUser(1L, "k", "code");

        assertThat(user.isActive()).isTrue();

        ReflectionTestUtils.setField(service, "redisUtil", redisUtil);
        when(redisUtil.getCache("k2")).thenReturn(null);
        User user2 = activeUser(2L);
        user2.setActive(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        service.activeUser(2L, "k2", "code");

        assertThat(user2.isActive()).isTrue();
    }

    @Test
    @DisplayName("checkUserValid：锁定时间已过期时允许登录")
    void checkUserValidAllowsExpiredLock() {
        User user = activeUser(1L);
        user.setLockoutEndDateUtc(LocalDateTime.now().minusHours(1));

        assertThatCode(() -> service.checkUserValid(user)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkUserIsActive：激活开始日期已过时忽略该限制")
    void checkUserIsActiveWithPastStartDate() {
        User user = activeUser(1L);
        user.setActiveStartDate(LocalDate.now().minusDays(1));
        user.setActiveEndDate(LocalDate.now().plusDays(1));

        assertThat(service.checkUserIsActive(user)).isTrue();
    }

    @Test
    @DisplayName("getForgetPwdCaptchaByMobile：租户策略为 mobile / mobileOrEmail 时允许发送")
    void getForgetPwdCaptchaByMobileAllowedPolicies() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.getValue(com.dusk.module.auth.feature.LoginFeatureProvider.APP_LOGIN_FORGET_PWD))
                .thenReturn("mobile", "mobileOrEmail");
        Tenant tenant = new Tenant();
        tenant.setActive(true);
        when(tenantRepository.findById(9L)).thenReturn(Optional.of(tenant));
        User user = activeUser(1L);
        user.setTenantId(9L);
        user.setPhoneNo("13900000000");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));

        service.getForgetPwdCaptchaByMobile("user1", "13900000000");
        service.getForgetPwdCaptchaByMobile("user1", "13900000000");

        verify(pushRpcService, times(2)).smsPushAsync(any());
        verify(redisUtil, times(4)).setCache(anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("getForgetPwdCaptchaByEmail：租户策略为 email / mobileOrEmail 时允许发送并使用租户名做标题")
    void getForgetPwdCaptchaByEmailAllowedPolicies() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.getValue(com.dusk.module.auth.feature.LoginFeatureProvider.APP_LOGIN_FORGET_PWD))
                .thenReturn("email", "mobileOrEmail");
        Tenant tenant = new Tenant();
        tenant.setActive(true);
        tenant.setName("租户A");
        when(tenantRepository.findById(9L)).thenReturn(Optional.of(tenant));
        Tenant host = new Tenant();
        host.setName("租户A");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(host));
        User user = activeUser(1L);
        user.setTenantId(9L);
        user.setEmailAddress("a@b.c");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));

        service.getForgetPwdCaptchaByEmail("user1", "a@b.c");
        service.getForgetPwdCaptchaByEmail("user1", "a@b.c");

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(emailRpcService, times(2)).sendEmail(subject.capture(), anyString(), org.mockito.ArgumentMatchers.eq("a@b.c"));
        assertThat(subject.getAllValues()).allMatch(s -> s.startsWith("租户A"));
    }

    @Test
    @DisplayName("getForgetPwdCaptchaByEmail：租户不存在时抛业务异常")
    void getForgetPwdCaptchaByEmailThrowsWhenTenantMissing() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.getValue(com.dusk.module.auth.feature.LoginFeatureProvider.APP_LOGIN_FORGET_PWD))
                .thenReturn("email");
        Tenant tenant = new Tenant();
        tenant.setActive(true);
        when(tenantRepository.findById(9L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.findById(1L)).thenReturn(Optional.empty());
        User user = activeUser(1L);
        user.setTenantId(9L);
        user.setEmailAddress("a@b.c");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.getForgetPwdCaptchaByEmail("user1", "a@b.c"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("租户不存在或已被删除");
    }

    @Test
    @DisplayName("getForgetPwdCaptchaByEmail：邮件 RPC 抛业务异常时原样抛出，其他异常包装")
    void getForgetPwdCaptchaByEmailPropagatesSendFailures() {
        User user = activeUser(1L);
        user.setEmailAddress("a@b.c");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));

        doThrow(new BusinessException("业务失败"))
                .when(emailRpcService).sendEmail(anyString(), anyString(), anyString());
        assertThatThrownBy(() -> service.getForgetPwdCaptchaByEmail("user1", "a@b.c"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("业务失败");

        doThrow(new RuntimeException("smtp down"))
                .when(emailRpcService).sendEmail(anyString(), anyString(), anyString());
        assertThatThrownBy(() -> service.getForgetPwdCaptchaByEmail("user1", "a@b.c"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮件发送失败");
    }

    @Test
    @DisplayName("resetPwdWithCaptcha：租户用户使用租户维度缓存 key")
    void resetPwdWithCaptchaTenantUser() {
        User user = activeUser(1L);
        user.setTenantId(9L);
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));
        when(redisUtil.getCache("CRUX:PWD:RESET:9:user1")).thenReturn("123456");
        when(passwordEncoder.encode("np")).thenReturn("H");

        service.resetPwdWithCaptcha("user1", "np", "123456");

        assertThat(user.getPassword()).isEqualTo("H");
        verify(redisUtil).deleteCache("CRUX:PWD:RESET:9:user1");
    }

    @Test
    @DisplayName("updatePersonalInfo：手机号格式错误 / 重复时抛业务异常")
    void updatePersonalInfoValidatesPhone() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser(1L)));
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        PersonalInfoInput invalid = new PersonalInfoInput();
        invalid.setId(1L);
        invalid.setName("n");
        invalid.setPhoneNo("123");
        assertThatThrownBy(() -> service.updatePersonalInfo(invalid))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号码格式错误");

        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(activeUser(9L)));
        PersonalInfoInput duplicate = new PersonalInfoInput();
        duplicate.setId(1L);
        duplicate.setName("n");
        duplicate.setPhoneNo("13800000000");
        assertThatThrownBy(() -> service.updatePersonalInfo(duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    @DisplayName("updatePersonalInfo：合法手机号与身份证号通过校验")
    void updatePersonalInfoAcceptsValidPhoneAndIdCard() {
        User user = activeUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(organizationUnitService.findAllById(any())).thenReturn(new ArrayList<>());

        PersonalInfoInput input = new PersonalInfoInput();
        input.setId(1L);
        input.setName("n");
        input.setPhoneNo("13800000000");
        input.setIdCard("110101199001011234");
        input.setOrgaId(null);

        service.updatePersonalInfo(input);

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updatePersonalInfo：身份证格式错误 / 重复时抛业务异常")
    void updatePersonalInfoValidatesIdCard() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser(1L)));
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        PersonalInfoInput invalid = new PersonalInfoInput();
        invalid.setId(1L);
        invalid.setName("n");
        invalid.setIdCard("abc");
        assertThatThrownBy(() -> service.updatePersonalInfo(invalid))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("身份证号码格式不正确");

        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(activeUser(9L)));
        PersonalInfoInput duplicate = new PersonalInfoInput();
        duplicate.setId(1L);
        duplicate.setName("n");
        duplicate.setIdCard("110101199001011234");
        assertThatThrownBy(() -> service.updatePersonalInfo(duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("身份证号码已被占用");
    }

    @Test
    @DisplayName("createExternalUser：外单位用户手机号/身份证为空且受控时抛业务异常")
    void createExternalUserRequiresPhoneAndIdCardWhenConfigured() {
        when(settingRpcService.getValue(UserServiceImpl.PERSONNEL_CONTROL_CONFIRM_PHONE)).thenReturn("true");
        CreateExternalUserInput noPhone = new CreateExternalUserInput();
        noPhone.setName("ext");
        assertThatThrownBy(() -> service.createExternalUser(noPhone))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号码不能为空");

        when(settingRpcService.getValue(UserServiceImpl.PERSONNEL_CONTROL_CONFIRM_PHONE)).thenReturn(null);
        when(settingRpcService.getValue(UserServiceImpl.PERSONNEL_CONTROL_CONFIRM_ID_CARD)).thenReturn("true");
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        CreateExternalUserInput noIdCard = new CreateExternalUserInput();
        noIdCard.setName("ext");
        noIdCard.setPhoneNo("13800000000");
        assertThatThrownBy(() -> service.createExternalUser(noIdCard))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("身份证号码不能为空");
    }

    @Test
    @DisplayName("updateUserInfoBySelf：类型为空时即使开启手机号管控也不校验必填")
    void updateUserInfoBySelfSkipsPhoneRequiredForUnknownType() {
        LoginUserIdContextHolder.setUserId(1L);
        when(settingRpcService.getValue(UserServiceImpl.PERSONNEL_CONTROL_CONFIRM_PHONE)).thenReturn("true");
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser(1L)));

        UpdateUserInfo info = new UpdateUserInfo();
        info.setName("self");

        service.updateUserInfoBySelf(info);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("checkAndLockUser：未锁定递增次数、达到上限锁定、锁定过期解锁")
    void checkAndLockUserBranches() {
        User first = activeUser(1L);
        first.setAccessFailedCount(null);
        first.setLockoutEndDateUtc(null);
        String firstMsg = ReflectionTestUtils.invokeMethod(service, "checkAndLockUser", first, 3);
        assertThat(firstMsg).contains("还有3次机会");
        assertThat(first.getAccessFailedCount()).isEqualTo(1);

        User locked = activeUser(2L);
        locked.setAccessFailedCount(3);
        locked.setLockoutEndDateUtc(null);
        String lockedMsg = ReflectionTestUtils.invokeMethod(service, "checkAndLockUser", locked, 3);
        assertThat(lockedMsg).contains("已经被锁定");
        assertThat(locked.getLockoutEndDateUtc()).isAfter(LocalDateTime.now());

        User expired = activeUser(3L);
        expired.setLockoutEndDateUtc(LocalDateTime.now().minusHours(1));
        when(userRepository.findById(3L)).thenReturn(Optional.of(expired));
        String expiredMsg = ReflectionTestUtils.invokeMethod(service, "checkAndLockUser", expired, 3);
        assertThat(expiredMsg).contains("还有3次机会");

        User future = activeUser(4L);
        future.setLockoutEndDateUtc(LocalDateTime.now().plusHours(2));
        String futureMsg = ReflectionTestUtils.invokeMethod(service, "checkAndLockUser", future, 3);
        assertThat(futureMsg).isEqualTo("帐户名或密码错误！");
    }

    @Test
    @DisplayName("checkUserAge：男女超龄与未超龄分支")
    void checkUserAgeBranches() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "checkUserAge", "110101196001011122"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("女55岁");

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(service, "checkUserAge", "110101200001011122"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "checkUserAge", "110101195001011111"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("男60岁");

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(service, "checkUserAge", "110101199001011111"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("createUser：租户用户上限特性关闭时不影响创建")
    void createUserWithMaxUsersFeatureDisabled() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.isEnabled(UserFeatureProvider.APP_USER_MAX_USERS)).thenReturn(false);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(403L);
            return u;
        });

        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("张三");
        dto.setPassword("p");
        dto.setUserName("u");
        input.setUser(dto);

        assertThat(service.createUser(input, EUnitType.Inner)).isEqualTo(403L);
    }

    @Test
    @DisplayName("createUser：用户数未达上限时允许创建")
    void createUserUnderMaxUsers() {
        TenantContextHolder.setTenantId(1L);
        when(featureChecker.isEnabled(UserFeatureProvider.APP_USER_MAX_USERS)).thenReturn(true);
        when(featureChecker.getValue(UserFeatureProvider.APP_USER_MAX_USERS_NUMBER)).thenReturn("10");
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(404L);
            return u;
        });

        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("张三");
        dto.setPassword("p");
        dto.setUserName("u");
        input.setUser(dto);

        assertThat(service.createUser(input, EUnitType.Inner)).isEqualTo(404L);
    }

    @Test
    @DisplayName("createUser：未激活时清空激活日期区间")
    void createUserClearsDatesWhenInactive() {
        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("张三");
        dto.setPassword("p");
        dto.setUserName("u");
        dto.setActive(false);
        dto.setActiveStartDate(LocalDate.now());
        dto.setActiveEndDate(LocalDate.now().plusDays(1));
        input.setUser(dto);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(300L);
            return u;
        });

        service.createUser(input, EUnitType.Inner);

        assertThat(captor.getValue().getActiveStartDate()).isNull();
        assertThat(captor.getValue().getActiveEndDate()).isNull();
    }

    @Test
    @DisplayName("createUser：激活结束日期早于开始日期时抛业务异常")
    void createUserThrowsWhenActiveEndBeforeStart() {
        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("张三");
        dto.setPassword("p");
        dto.setUserName("u");
        dto.setActive(true);
        dto.setActiveStartDate(LocalDate.now());
        dto.setActiveEndDate(LocalDate.now().minusDays(1));
        input.setUser(dto);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createUser(input, EUnitType.Inner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("激活结束日期必须大于起始日期");
    }

    @Test
    @DisplayName("createUser：激活区间合法或仅缺一个端点时允许创建")
    void createUserAllowsValidOrPartialActiveRange() {
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(301L);
            return u;
        });

        CreateOrUpdateUserInput both = new CreateOrUpdateUserInput();
        UserEditDto bothDto = new UserEditDto();
        bothDto.setName("张三");
        bothDto.setPassword("p");
        bothDto.setUserName("u1");
        bothDto.setActive(true);
        bothDto.setActiveStartDate(LocalDate.now());
        bothDto.setActiveEndDate(LocalDate.now().plusDays(1));
        both.setUser(bothDto);
        assertThat(service.createUser(both, EUnitType.Inner)).isEqualTo(301L);

        CreateOrUpdateUserInput onlyEnd = new CreateOrUpdateUserInput();
        UserEditDto endDto = new UserEditDto();
        endDto.setName("李四");
        endDto.setPassword("p");
        endDto.setUserName("u2");
        endDto.setActive(true);
        endDto.setActiveEndDate(LocalDate.now().plusDays(1));
        onlyEnd.setUser(endDto);
        assertThat(service.createUser(onlyEnd, EUnitType.Inner)).isEqualTo(301L);

        CreateOrUpdateUserInput onlyStart = new CreateOrUpdateUserInput();
        UserEditDto startDto = new UserEditDto();
        startDto.setName("王五");
        startDto.setPassword("p");
        startDto.setUserName("u3");
        startDto.setActive(true);
        startDto.setActiveStartDate(LocalDate.now().minusDays(1));
        onlyStart.setUser(startDto);
        assertThat(service.createUser(onlyStart, EUnitType.Inner)).isEqualTo(301L);
    }

    @Test
    @DisplayName("createUser：用户名 / 工号重复时抛业务异常")
    void createUserThrowsOnDuplicateUserNameOrWorkNumber() {
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(activeUser(9L)));
        CreateOrUpdateUserInput duplicateName = new CreateOrUpdateUserInput();
        UserEditDto nameDto = new UserEditDto();
        nameDto.setName("张三");
        nameDto.setPassword("p");
        nameDto.setUserName("dup");
        duplicateName.setUser(nameDto);
        assertThatThrownBy(() -> service.createUser(duplicateName, EUnitType.Inner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已被占用");

        when(userRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.empty(), Optional.of(activeUser(9L)));
        CreateOrUpdateUserInput duplicateWorkNumber = new CreateOrUpdateUserInput();
        UserEditDto workDto = new UserEditDto();
        workDto.setName("张三");
        workDto.setPassword("p");
        workDto.setUserName("u");
        workDto.setWorkNumber("w-dup");
        duplicateWorkNumber.setUser(workDto);
        assertThatThrownBy(() -> service.createUser(duplicateWorkNumber, EUnitType.Inner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工号已被占用");
    }

    @Test
    @DisplayName("createUser：工号非空且唯一时允许创建")
    void createUserAllowsUniqueWorkNumber() {
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(405L);
            return u;
        });

        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("张三");
        dto.setPassword("p");
        dto.setUserName("u");
        dto.setWorkNumber("w-unique");
        input.setUser(dto);

        assertThat(service.createUser(input, EUnitType.Inner)).isEqualTo(405L);
    }

    @Test
    @DisplayName("createUser：指定角色时推进代际并发送激活邮件")
    void createUserPublishesSubjectAndSendsActivationEmail() {
        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("张三");
        dto.setPassword("p");
        dto.setUserName("u");
        dto.setEmailAddress("a@b.c");
        input.setUser(dto);
        input.setAssignedRoleIds(List.of(5L));
        input.setSendActivationEmail(true);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        Role role = new Role();
        role.setId(5L);
        when(roleService.findAllById(List.of(5L))).thenReturn(List.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(302L);
            return u;
        });
        when(authzSubjectVersionService.nextVersion(anyString(), org.mockito.ArgumentMatchers.eq(302L)))
                .thenReturn(3L);

        assertThat(service.createUser(input, EUnitType.Inner)).isEqualTo(302L);

        verify(authzSubjectVersionService).nextVersion(anyString(), org.mockito.ArgumentMatchers.eq(302L));
        verify(emailRpcService).sendEmail(org.mockito.ArgumentMatchers.eq("账号激活通知"),
                org.mockito.ArgumentMatchers.contains("key="),
                org.mockito.ArgumentMatchers.eq("a@b.c"));
        verify(redisUtil).setCache(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(900L));
    }

    @Test
    @DisplayName("createUser：发送激活邮件失败时包装为业务异常")
    void createUserWrapsActivationMailFailure() {
        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("张三");
        dto.setPassword("p");
        dto.setUserName("u");
        dto.setEmailAddress("a@b.c");
        input.setUser(dto);
        input.setSendActivationEmail(true);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(303L);
            return u;
        });
        doThrow(new RuntimeException("smtp down"))
                .when(emailRpcService).sendEmail(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.createUser(input, EUnitType.Inner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮件发送失败");
    }

    @Test
    @DisplayName("createUser：redisUtil 为空时激活邮件不带 key")
    void createUserSendsActivationEmailWithoutRedis() {
        ReflectionTestUtils.setField(service, "redisUtil", null);
        CreateOrUpdateUserInput input = new CreateOrUpdateUserInput();
        UserEditDto dto = new UserEditDto();
        dto.setName("张三");
        dto.setPassword("p");
        dto.setUserName("u");
        dto.setEmailAddress("a@b.c");
        input.setUser(dto);
        input.setSendActivationEmail(true);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(304L);
            return u;
        });

        service.createUser(input, EUnitType.Inner);

        verify(emailRpcService).sendEmail(org.mockito.ArgumentMatchers.eq("账号激活通知"),
                org.mockito.ArgumentMatchers.eq("http://active?userId=304"),
                org.mockito.ArgumentMatchers.eq("a@b.c"));
    }

    @Test
    @DisplayName("generateToken：用户存在时签发令牌")
    void generateToken() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser(1L)));
        when(tokenAuthManager.generateToken(any(UserContext.class))).thenReturn("T");

        assertThat(service.generateToken(1L)).isEqualTo("T");
    }

    @Test
    @DisplayName("decryptSm4：密钥合法时成功解密")
    void decryptSm4SucceedsWithValidKey() {
        String key = "0123456789abcdeffedcba9876543210";
        when(appAuthConfig.getLoginEncryptKey()).thenReturn(key);
        String encrypted = SmUtil.sm4(HexUtil.decodeHex(key)).encryptHex("secret");

        assertThat(service.decryptSm4(encrypted, "err")).isEqualTo("secret");
    }
}
