package com.dusk.module.auth.service.impl;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.enums.UserStatus;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.exception.UserLoginException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.mqs.core.MessageSender;
import com.dusk.module.auth.dto.ChangePwdInput;
import com.dusk.module.auth.dto.CreateOrUpdateUserInput;
import com.dusk.module.auth.dto.UserEditDto;
import com.dusk.module.auth.dto.orga.GetOrganizationUnitUsersInput;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.module.auth.common.config.AppAuthConfig;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.dto.user.ChangePasswordInput;
import com.dusk.module.auth.dto.user.ChangeStatusInput;
import com.dusk.module.auth.dto.user.CreateExternalUserInput;
import com.dusk.module.auth.dto.user.CreateOrUpdateUserInfoInput;
import com.dusk.module.auth.dto.user.ExternalUserSettingDto;
import com.dusk.module.auth.dto.user.GetOrgaUsersInput;
import com.dusk.module.auth.dto.user.GetUserForEditOutput;
import com.dusk.module.auth.dto.user.GetUserInfoOutput;
import com.dusk.module.auth.dto.user.GetUsersByRoleCodesInput;
import com.dusk.module.auth.dto.user.GetUsersByRoleNameInput;
import com.dusk.module.auth.dto.user.GetUsersForLoginInput;
import com.dusk.module.auth.dto.user.GetUsersInput;
import com.dusk.module.auth.dto.user.PersonalInfoInput;
import com.dusk.module.auth.dto.user.SetDefaultStationInput;
import com.dusk.module.auth.dto.user.UpdateUserInfo;
import com.dusk.module.auth.dto.user.UserInfoDto;
import com.dusk.module.auth.common.util.LoginUtils;
import com.dusk.module.auth.entity.OrganizationManager;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.enums.ELevel;
import com.dusk.module.auth.feature.UserFeatureProvider;
import com.dusk.module.auth.push.INotificationPushManager;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.repository.IOrganizationManagerRepository;
import com.dusk.module.auth.repository.ITenantRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.IEmailService;
import com.dusk.module.auth.service.IFeatureChecker;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.IRoleService;
import com.dusk.module.auth.service.IStationService;
import com.dusk.module.metadata.service.ISettingRpcService;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.common.mqs.pusher.SmsPushConfig;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private INotificationPushManager pushManager;
    @Mock
    private IEmailService emailService;
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

    private UserServiceImpl service;

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
        ReflectionTestUtils.setField(service, "pushManager", pushManager);
        ReflectionTestUtils.setField(service, "emailService", emailService);
        ReflectionTestUtils.setField(service, "tokenAuthManager", tokenAuthManager);
        ReflectionTestUtils.setField(service, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(service, "organizationManagerRepository", organizationManagerRepository);
        ReflectionTestUtils.setField(service, "stationService", stationService);
        ReflectionTestUtils.setField(service, "settingRpcService", settingRpcService);
        ReflectionTestUtils.setField(service, "sender", sender);
        ReflectionTestUtils.setField(service, "passwdLen", 8);
        ReflectionTestUtils.setField(service, "activeAddr", "http://active");
        when(appAuthConfig.getLoginEncryptKey()).thenReturn(null);
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
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

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
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.createUser(input, EUnitType.Inner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮件发送失败");
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
    @DisplayName("getForgetPwdCaptchaByMobile：未启用推送/租户策略不允许/手机号不匹配/发送过频均抛异常")
    void getForgetPwdCaptchaByMobileGuards() {
        ReflectionTestUtils.setField(service, "pushManager", null);
        assertThatThrownBy(() -> service.getForgetPwdCaptchaByMobile("u", "13800000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("消息推送服务未启用");
        ReflectionTestUtils.setField(service, "pushManager", pushManager);

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
    @DisplayName("getForgetPwdCaptchaByMobile：正常发送验证码并写入缓存")
    void getForgetPwdCaptchaByMobileSends() {
        User user = activeUser(1L);
        user.setPhoneNo("13900000000");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));

        service.getForgetPwdCaptchaByMobile("user1", "13900000000");

        verify(pushManager).smsPushAsync(any());
        verify(redisUtil, times(2)).setCache(anyString(), any(), anyLong());
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
    @DisplayName("getForgetPwdCaptchaByEmail：正常发送验证码")
    void getForgetPwdCaptchaByEmailSends() {
        User user = activeUser(1L);
        user.setEmailAddress("a@b.c");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));

        service.getForgetPwdCaptchaByEmail("user1", "a@b.c");

        verify(emailService).sendEmail(anyString(), anyString(), anyString());
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
}
