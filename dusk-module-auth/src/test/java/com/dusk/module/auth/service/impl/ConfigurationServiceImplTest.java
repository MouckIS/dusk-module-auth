package com.dusk.module.auth.service.impl;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.common.config.AppAuthConfig;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.dto.configuration.ConfigurationDto;
import com.dusk.module.auth.dto.user.GetUserForEditOutput;
import com.dusk.module.auth.dto.user.UserRoleDto;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.repository.ITenantRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ICaptchaService;
import com.dusk.module.auth.service.IFeatureService;
import com.dusk.module.auth.service.ITenantPermissionService;
import com.dusk.module.auth.service.IUserService;
import com.dusk.module.metadata.dto.DynamicMenuDto;
import com.dusk.module.metadata.service.IDynamicMenuRpcService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ConfigurationServiceImpl} 单元测试，目标：100% 分支覆盖。
 *
 * <p>重点覆盖 {@code getAll} 中「匿名访问 / 宿主访问 / 租户访问」三条权限清单获取路径，
 * 以及用户信息、动态菜单、租户配置的可空分支。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfigurationServiceImplTest {

    private static final long TENANT_ID = 66L;
    private static final long USER_ID = 12L;

    @Mock
    private IUserRepository userRepository;
    @Mock
    private IAuthPermissionManager authPermissionManager;
    @Mock
    private IFeatureService featureService;
    @Mock
    private ITenantRepository tenantRepository;
    @Mock
    private IGrantPermissionRepository grantPermissionRepository;
    @Mock
    private ITenantPermissionService tenantPermissionService;
    @Mock
    private ICaptchaService captchaService;
    @Mock
    private AppAuthConfig appAuthConfig;
    @Mock
    private IUserService userService;
    @Mock
    private TokenAuthManager tokenAuthManager;
    @Mock
    private IDynamicMenuRpcService dynamicMenuRpcService;

    private ConfigurationServiceImpl service;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        service = new ConfigurationServiceImpl();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "authPermissionManager", authPermissionManager);
        ReflectionTestUtils.setField(service, "featureService", featureService);
        ReflectionTestUtils.setField(service, "tenantRepository", tenantRepository);
        ReflectionTestUtils.setField(service, "grantPermissionRepository", grantPermissionRepository);
        ReflectionTestUtils.setField(service, "tenantPermissionService", tenantPermissionService);
        ReflectionTestUtils.setField(service, "captchaService", captchaService);
        ReflectionTestUtils.setField(service, "appAuthConfig", appAuthConfig);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "tokenAuthManager", tokenAuthManager);
        ReflectionTestUtils.setField(service, "dynamicMenuRpcService", dynamicMenuRpcService);

        request = new MockHttpServletRequest();
        when(featureService.getTenantFeatures()).thenReturn(Map.of());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private static UserContext userContext() {
        UserContext context = new UserContext();
        context.setId(USER_ID);
        return context;
    }

    private static User loginUser(boolean admin, List<Role> roles) {
        User user = new User();
        user.setId(USER_ID);
        user.setName("tester");
        user.setAdmin(admin);
        user.setUserRoles(new ArrayList<>(roles));
        return user;
    }

    private static Role role(long id) {
        Role role = new Role();
        role.setId(id);
        return role;
    }

    private static GrantPermission grantPermission(String name) {
        GrantPermission permission = new GrantPermission();
        permission.setName(name);
        return permission;
    }

    private static GetUserForEditOutput userInfoWithRoles(boolean assigned, Long... roleIds) {
        GetUserForEditOutput output = new GetUserForEditOutput();
        List<UserRoleDto> roles = new ArrayList<>();
        for (Long roleId : roleIds) {
            UserRoleDto dto = new UserRoleDto();
            dto.setRoleId(roleId);
            dto.setAssigned(assigned);
            roles.add(dto);
        }
        output.setRoles(roles);
        return output;
    }

    // ---------------- 匿名访问 ----------------

    @Test
    @DisplayName("getAll：匿名访问（Token 无效）时不下发用户信息与动态菜单")
    void getAllForAnonymous() {
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(null);
        when(authPermissionManager.getDefinitionPermission(false))
                .thenReturn(List.of("Pages.Administration"));

        ConfigurationDto result = service.getAll(request);

        assertThat(result.getCurrentUser()).isNull();
        assertThat(result.getUserInfo()).isNull();
        assertThat(result.getDynamicMenus()).isNull();
        assertThat(result.getAuth()).isEmpty();
        verify(userService, never()).getUserForEdit(any(EntityDto.class));
    }

    @Test
    @DisplayName("getAll：Token 校验抛异常时被吞掉并按匿名处理")
    void getAllSwallowsTokenException() {
        when(tokenAuthManager.checkTokenValid(request)).thenThrow(new RuntimeException("token 失效"));
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of());

        ConfigurationDto result = service.getAll(request);

        assertThat(result.getCurrentUser()).isNull();
    }

    // ---------------- 宿主 / 租户权限清单 ----------------

    @Test
    @DisplayName("getAll：宿主访问（无租户上下文）时取全量权限清单")
    void getAllForHost() {
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(null);
        when(authPermissionManager.getDefinitionPermission(false))
                .thenReturn(List.of("Pages.A", "Pages.B"));

        assertThat(service.getAll(request).getAuth()).isEmpty();
        verify(authPermissionManager).getDefinitionPermission(false);
    }

    @Test
    @DisplayName("getAll：租户访问时取该租户已授权权限清单")
    void getAllForTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(null);
        when(tenantPermissionService.getGrantedPermissionByTenantId(TENANT_ID))
                .thenReturn(List.of("Pages.A"));

        assertThat(service.getAll(request).getAuth()).isEmpty();
        verify(tenantPermissionService).getGrantedPermissionByTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("getAll：disableTenantAuthFilter=true 时按是否登录取清单")
    void getAllWhenTenantFilterDisabled() {
        when(appAuthConfig.isDisableTenantAuthFilter()).thenReturn(true);
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(null);
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of());

        service.getAll(request);

        verify(authPermissionManager).getDefinitionPermission(false);
        verify(tenantPermissionService, never()).getGrantedPermissionByTenantId(anyLong());
    }

    @Test
    @DisplayName("getAll：disableTenantAuthFilter=true 且有租户上下文时请求含租户的清单")
    void getAllWhenTenantFilterDisabledWithTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(appAuthConfig.isDisableTenantAuthFilter()).thenReturn(true);
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(null);
        when(authPermissionManager.getDefinitionPermission(true)).thenReturn(List.of());

        service.getAll(request);

        verify(authPermissionManager).getDefinitionPermission(true);
    }

    // ---------------- 已登录用户 ----------------

    @Test
    @DisplayName("getAll：管理员用户授予全部权限")
    void getAllGrantsAllForAdmin() {
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(userContext());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(loginUser(true, List.of())));
        when(authPermissionManager.getDefinitionPermission(false))
                .thenReturn(List.of("Pages.A", "Pages.B"));

        ConfigurationDto result = service.getAll(request);

        assertThat(result.getAuth()).hasSize(2);
        assertThat(result.getAuth().getFirst().isGranted()).isTrue();
    }

    @Test
    @DisplayName("getAll：非管理员仅授予已授权的权限")
    void getAllGrantsOnlyGrantedForNormalUser() {
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(userContext());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(loginUser(false, List.of(role(5L)))));
        when(grantPermissionRepository.findDistinctByRoleIdIn(any(Long[].class)))
                .thenReturn(List.of(grantPermission("Pages.A"), grantPermission("Pages.A")));
        when(authPermissionManager.getDefinitionPermission(false))
                .thenReturn(List.of("Pages.A", "Pages.B"));

        ConfigurationDto result = service.getAll(request);

        assertThat(result.getAuth()).hasSize(1);
        assertThat(result.getAuth().getFirst().getPermissionCode()).isEqualTo("Pages.A");
    }

    @Test
    @DisplayName("getAll：非管理员无角色时不查询授权表")
    void getAllSkipsGrantQueryWithoutRoles() {
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(userContext());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(loginUser(false, List.of())));
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of("Pages.A"));

        assertThat(service.getAll(request).getAuth()).isEmpty();
        verify(grantPermissionRepository, never()).findDistinctByRoleIdIn(any(Long[].class));
    }

    @Test
    @DisplayName("getAll：用户已被删除时不授予任何权限")
    void getAllWhenUserNotFound() {
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(userContext());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of("Pages.A"));

        assertThat(service.getAll(request).getAuth()).isEmpty();
    }

    // ---------------- 用户信息与动态菜单 ----------------

    @Test
    @DisplayName("getAll：已登录时下发用户信息，并按已分配角色查询动态菜单")
    void getAllReturnsUserInfoAndDynamicMenus() {
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(userContext());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(loginUser(true, List.of())));
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of());
        when(userService.getUserForEdit(any(EntityDto.class)))
                .thenReturn(userInfoWithRoles(true, 7L, 8L));
        when(dynamicMenuRpcService.getDynamicMenus(List.of(7L, 8L)))
                .thenReturn(List.of(new DynamicMenuDto()));

        ConfigurationDto result = service.getAll(request);

        assertThat(result.getUserInfo()).isNotNull();
        assertThat(result.getDynamicMenus()).hasSize(1);
    }

    @Test
    @DisplayName("getAll：无已分配角色时不查询动态菜单")
    void getAllSkipsDynamicMenusWhenNoAssignedRole() {
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(userContext());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(loginUser(true, List.of())));
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of());
        when(userService.getUserForEdit(any(EntityDto.class)))
                .thenReturn(userInfoWithRoles(false, 7L));

        ConfigurationDto result = service.getAll(request);

        assertThat(result.getDynamicMenus()).isNull();
        verify(dynamicMenuRpcService, never()).getDynamicMenus(any());
    }

    // ---------------- 租户配置与登录信息 ----------------

    @Test
    @DisplayName("getAll：有租户上下文且租户存在时下发租户配置")
    void getAllReturnsTenantConfig() {
        TenantContextHolder.setTenantId(TENANT_ID);
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName("演示租户");
        tenant.setTenantName("demo");
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(null);
        when(authPermissionManager.getDefinitionPermission(anyBoolean())).thenReturn(List.of());
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(captchaService.checkNeedCaptcha(request)).thenReturn(true);

        ConfigurationDto result = service.getAll(request);

        assertThat(result.getTenantConfig()).isNotNull();
        assertThat(result.getTenantConfig().getName()).isEqualTo("演示租户");
        assertThat(result.getLoginInfo().isNeedCaptcha()).isTrue();
    }

    @Test
    @DisplayName("getAll：有租户上下文但租户不存在时租户配置为空")
    void getAllWithoutTenantConfigWhenTenantAbsent() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(null);
        when(authPermissionManager.getDefinitionPermission(anyBoolean())).thenReturn(List.of());
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThat(service.getAll(request).getTenantConfig()).isNull();
    }

    @Test
    @DisplayName("getAll：无租户上下文时租户配置为空且不查询租户表")
    void getAllWithoutTenantContext() {
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(null);
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of());

        assertThat(service.getAll(request).getTenantConfig()).isNull();
        verify(tenantRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("getAll：特性配置始终下发全部租户特性")
    void getAllReturnsFeatureConfig() {
        when(tokenAuthManager.checkTokenValid(request)).thenReturn(null);
        when(authPermissionManager.getDefinitionPermission(false)).thenReturn(List.of());
        when(featureService.getTenantFeatures()).thenReturn(Map.of("AppUser", Map.of("AllowMobileLogin", "true")));

        ConfigurationDto result = service.getAll(request);

        assertThat(result.getFeatureConfig().getAllFeatures())
                .containsEntry("AppUser", Map.of("AllowMobileLogin", "true"));
    }
}
