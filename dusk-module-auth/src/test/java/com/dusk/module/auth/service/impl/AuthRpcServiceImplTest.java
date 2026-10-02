package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.permission.UrlPermission;
import com.dusk.common.core.exception.UserContextException;
import com.dusk.common.core.jwt.JwtTokenFactory;
import com.dusk.common.core.jwt.extractor.JwtHeaderTokenExtractor;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.common.core.utils.UserContextUtils;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.common.manage.DefaultAccessDecisionManager;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.metadata.DefaultInvocationSecurityMetadataSource;
import com.dusk.module.auth.common.provider.CustomAuthProvider;
import com.dusk.module.auth.common.skiprequest.SkipPathRequestMatcher;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.feature.CenterControlFeatureProvider;
import com.dusk.module.auth.service.IFeatureChecker;
import com.dusk.module.auth.service.IStationService;
import org.apache.dubbo.rpc.RpcContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthRpcServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthRpcServiceImplTest {

    @Mock
    private JwtTokenFactory jwtTokenFactory;
    @Mock
    private JwtHeaderTokenExtractor jwtHeaderTokenExtractor;
    @Mock
    private UserContextUtils userContextUtils;
    @Mock
    private DefaultAccessDecisionManager accessDecisionManager;
    @Mock
    private DefaultInvocationSecurityMetadataSource metadataSource;
    @Mock
    private CustomAuthProvider customAuthProvider;
    @Mock
    private SkipPathRequestMatcher skipPathRequestMatcher;
    @Mock
    private IDataFilterDefinitionContext dataFilterDefinitionContext;
    @Mock
    private TokenAuthManager tokenAuthManager;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private IStationService stationService;
    @Mock
    private IFeatureChecker featureChecker;

    private AuthRpcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthRpcServiceImpl();
        ReflectionTestUtils.setField(service, "jwtTokenFactory", jwtTokenFactory);
        ReflectionTestUtils.setField(service, "jwtHeaderTokenExtractor", jwtHeaderTokenExtractor);
        ReflectionTestUtils.setField(service, "userContextUtils", userContextUtils);
        ReflectionTestUtils.setField(service, "accessDecisionManager", accessDecisionManager);
        ReflectionTestUtils.setField(service, "metadataSource", metadataSource);
        ReflectionTestUtils.setField(service, "customAuthProvider", customAuthProvider);
        ReflectionTestUtils.setField(service, "skipPathRequestMatcher", skipPathRequestMatcher);
        ReflectionTestUtils.setField(service, "dataFilterDefinitionContext", dataFilterDefinitionContext);
        ReflectionTestUtils.setField(service, "tokenAuthManager", tokenAuthManager);
        ReflectionTestUtils.setField(service, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(service, "stationService", stationService);
        ReflectionTestUtils.setField(service, "featureChecker", featureChecker);
    }

    @AfterEach
    void tearDown() {
        RpcContext.removeContext();
    }

    private static StationsOfLoginUserDto station(long value, boolean defaultBy) {
        StationsOfLoginUserDto dto = new StationsOfLoginUserDto();
        dto.setValue(value);
        dto.setDefaultBy(defaultBy);
        return dto;
    }

    private void stubLoggedInUser() {
        UserContext context = new UserContext();
        context.setId(11L);
        context.setTenantId(22L);
        when(userContextUtils.getUserContext("token")).thenReturn(context);
    }

    // ---------------- auth ----------------

    @Test
    @DisplayName("auth：命中跳过路径时直接放行")
    void authReturnsTrueWhenPathIgnored() {
        when(skipPathRequestMatcher.matches("app", "/public")).thenReturn(true);

        assertThat(service.auth("token", "app", "/public")).isTrue();
        verify(tokenAuthManager, never()).checkTokenValid(anyString());
    }

    @Test
    @DisplayName("auth：未命中跳过路径时执行权限判定")
    void authDelegatesToPermissionDecision() {
        when(skipPathRequestMatcher.matches("app", "/api")).thenReturn(false);
        UserContext context = new UserContext();
        when(tokenAuthManager.checkTokenValid("token")).thenReturn(context);
        when(metadataSource.getAttributes("app", "/api")).thenReturn(List.of("perm"));
        when(accessDecisionManager.decide(any(), any())).thenReturn(true);

        assertThat(service.auth("token", "app", "/api")).isTrue();
    }

    @Test
    @DisplayName("auth：Token 无效时抛出未登录异常")
    void authThrowsWhenTokenInvalid() {
        when(skipPathRequestMatcher.matches("app", "/api")).thenReturn(false);
        when(tokenAuthManager.checkTokenValid("token")).thenReturn(null);

        assertThatThrownBy(() -> service.auth("token", "app", "/api"))
                .isInstanceOf(UserContextException.class);
    }

    // ---------------- provideAuthInfo ----------------

    @Test
    @DisplayName("provideAuthInfo：转发到自定义鉴权提供者")
    void provideAuthInfoDelegates() {
        Map<String, Permission> permissions = new HashMap<>();
        Map<String, List<UrlPermission>> urlPermissions = new HashMap<>();

        service.provideAuthInfo("app", List.of("/open"), permissions, urlPermissions);

        verify(customAuthProvider).provideAuthInfo("app", List.of("/open"), permissions, urlPermissions);
    }

    // ---------------- getUserContext ----------------

    @Test
    @DisplayName("getUserContext：无 authorization 头时返回 null")
    void getUserContextReturnsNullWhenNoAttachment() {
        assertThat(service.getUserContext()).isNull();
    }

    @Test
    @DisplayName("getUserContext：解析成功时返回用户上下文")
    void getUserContextParsesToken() {
        RpcContext.getContext().setAttachment("authorization", "Bearer abc");
        UserContext context = new UserContext();
        when(jwtHeaderTokenExtractor.extract("Bearer abc")).thenReturn("abc");
        when(jwtTokenFactory.parseJwtToken("abc")).thenReturn(context);

        assertThat(service.getUserContext()).isSameAs(context);
    }

    @Test
    @DisplayName("getUserContext：解析异常时返回 null")
    void getUserContextReturnsNullWhenParseFails() {
        RpcContext.getContext().setAttachment("authorization", "Bearer bad");
        when(jwtHeaderTokenExtractor.extract("Bearer bad")).thenReturn("bad");
        when(jwtTokenFactory.parseJwtToken("bad")).thenThrow(new IllegalArgumentException("bad token"));

        assertThat(service.getUserContext()).isNull();
    }

    // ---------------- changeRealToken ----------------

    @Test
    @DisplayName("changeRealToken：委托 TokenAuthManager 查询真实 Token")
    void changeRealTokenDelegates() {
        when(tokenAuthManager.getToken("id-1")).thenReturn("real-token");

        assertThat(service.changeRealToken("id-1")).isEqualTo("real-token");
    }

    // ---------------- getLinkedOrgIds ----------------

    @Test
    @DisplayName("getLinkedOrgIds：authentication 为空且 orgId 为空时返回 null")
    void getLinkedOrgIdsReturnsNullWhenAuthenticationEmpty() {
        assertThat(service.getLinkedOrgIds(null, null)).isNull();
        verify(userContextUtils, never()).getUserContext(anyString());
    }

    @Test
    @DisplayName("getLinkedOrgIds：解析不到用户上下文时返回 null")
    void getLinkedOrgIdsReturnsNullWhenUserContextAbsent() {
        when(userContextUtils.getUserContext("token")).thenReturn(null);

        assertThat(service.getLinkedOrgIds(null, "token")).isNull();
    }

    @Test
    @DisplayName("getLinkedOrgIds：开启厂站向下传递时直接返回 orgId")
    void getLinkedOrgIdsReturnsOrgIdWhenDownwardEnabled() {
        stubLoggedInUser();
        when(featureChecker.isEnabled(CenterControlFeatureProvider.STATION_DOWNWARD)).thenReturn(true);

        assertThat(service.getLinkedOrgIds("88", "token")).isEqualTo("88");
    }

    @Test
    @DisplayName("getLinkedOrgIds：已登录且已指定 orgId 时跳过默认厂站推导")
    void getLinkedOrgIdsSkipsDefaultStationWhenOrgIdProvided() {
        stubLoggedInUser();
        when(featureChecker.isEnabled(CenterControlFeatureProvider.STATION_DOWNWARD)).thenReturn(false);
        Map<String, List<Long>> definition = new HashMap<>();
        definition.put("5", List.of(50L));
        when(dataFilterDefinitionContext.getDataFilterDefinition()).thenReturn(definition);

        assertThat(service.getLinkedOrgIds("5", "token")).isEqualTo("50");
        verify(stationService, never()).getStationsForFrontByUserId(any());
    }

    @Test
    @DisplayName("getLinkedOrgIds：集控模式合并所有厂站关联组织")
    void getLinkedOrgIdsMergesAllOrgsInCenterControl() {
        stubLoggedInUser();
        when(featureChecker.isEnabled(CenterControlFeatureProvider.STATION_DOWNWARD)).thenReturn(false);
        when(featureChecker.isEnabled(CenterControlFeatureProvider.STATION_CENTER_CONTROL)).thenReturn(true);
        when(stationService.getStationsForFrontByUserId(11L))
                .thenReturn(List.of(station(1L, false), station(2L, false)));
        Map<String, List<Long>> definition = new HashMap<>();
        definition.put("1", List.of(10L, 11L));
        definition.put("2", List.of(11L, 12L));
        when(dataFilterDefinitionContext.getDataFilterDefinition()).thenReturn(definition);

        assertThat(service.getLinkedOrgIds("", "token")).isEqualTo("10,11,12");
    }

    @Test
    @DisplayName("getLinkedOrgIds：集控模式下关联组织为空时返回 null")
    void getLinkedOrgIdsReturnsNullWhenNoAllOrg() {
        stubLoggedInUser();
        when(featureChecker.isEnabled(CenterControlFeatureProvider.STATION_DOWNWARD)).thenReturn(false);
        when(featureChecker.isEnabled(CenterControlFeatureProvider.STATION_CENTER_CONTROL)).thenReturn(true);
        when(stationService.getStationsForFrontByUserId(11L)).thenReturn(List.of(station(1L, false)));
        Map<String, List<Long>> definition = new HashMap<>();
        definition.put("1", List.of());
        when(dataFilterDefinitionContext.getDataFilterDefinition()).thenReturn(definition);

        assertThat(service.getLinkedOrgIds("", "token")).isNull();
    }

    @Test
    @DisplayName("getLinkedOrgIds：非集控模式优先使用默认厂站")
    void getLinkedOrgIdsUsesDefaultStation() {
        stubLoggedInUser();
        when(featureChecker.isEnabled(CenterControlFeatureProvider.STATION_CENTER_CONTROL)).thenReturn(false);
        when(stationService.getStationsForFrontByUserId(11L))
                .thenReturn(List.of(station(5L, true), station(6L, false)));
        Map<String, List<Long>> definition = new HashMap<>();
        definition.put("5", List.of(50L));
        when(dataFilterDefinitionContext.getDataFilterDefinition()).thenReturn(definition);

        assertThat(service.getLinkedOrgIds("", "token")).isEqualTo("50");
    }

    @Test
    @DisplayName("getLinkedOrgIds：无默认厂站时取第一个厂站")
    void getLinkedOrgIdsUsesFirstStation() {
        stubLoggedInUser();
        when(featureChecker.isEnabled(CenterControlFeatureProvider.STATION_CENTER_CONTROL)).thenReturn(false);
        when(stationService.getStationsForFrontByUserId(11L))
                .thenReturn(List.of(station(7L, false), station(8L, false)));
        Map<String, List<Long>> definition = new HashMap<>();
        definition.put("7", List.of(70L));
        when(dataFilterDefinitionContext.getDataFilterDefinition()).thenReturn(definition);

        assertThat(service.getLinkedOrgIds("", "token")).isEqualTo("70");
    }

    @Test
    @DisplayName("getLinkedOrgIds：厂站列表为空时返回 null")
    void getLinkedOrgIdsReturnsNullWhenNoStations() {
        stubLoggedInUser();
        when(featureChecker.isEnabled(CenterControlFeatureProvider.STATION_CENTER_CONTROL)).thenReturn(false);
        when(stationService.getStationsForFrontByUserId(11L)).thenReturn(List.of());

        assertThat(service.getLinkedOrgIds("", "token")).isNull();
    }

    @Test
    @DisplayName("getLinkedOrgIds：orgId 无关联组织映射时返回 null")
    void getLinkedOrgIdsReturnsNullWhenIdsAbsent() {
        when(dataFilterDefinitionContext.getDataFilterDefinition()).thenReturn(new HashMap<>());

        assertThat(service.getLinkedOrgIds("99", null)).isNull();
    }

    @Test
    @DisplayName("getLinkedOrgIds：orgId 关联组织为空集合时返回 null")
    void getLinkedOrgIdsReturnsNullWhenIdsEmpty() {
        Map<String, List<Long>> definition = new HashMap<>();
        definition.put("99", List.of());
        when(dataFilterDefinitionContext.getDataFilterDefinition()).thenReturn(definition);

        assertThat(service.getLinkedOrgIds("99", null)).isNull();
    }
}
