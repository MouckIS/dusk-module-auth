package com.dusk.module.auth.common.permission.impl;

import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.permission.RoleInfo;
import com.dusk.common.core.auth.permission.UrlPermission;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.module.auth.service.IGrantPermissionService;
import com.dusk.module.auth.service.ITenantPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link RedisPermissionCache} 单元测试：覆盖匿名路径/权限定义/URL 权限的 Redis 读写、
 * 旧 key 清理，以及 refreshAll 在授权服务缺失或租户服务缺失时的分支。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisPermissionCacheTest {

    private static final String DEFINITION_KEY = "CRUX:AUTH:PERMISSION:DEFINITION";
    private static final String URL_KEY_OLD = "CRUX:AUTH:PERMISSION:URL";
    private static final String URL_KEY = "CRUX:AUTH:PERMISSION:URLS";
    private static final String ANONYMOUS_KEY = "CRUX:AUTH:PERMISSION:ANONYMOUS";

    @Mock
    private RedisUtil<Object> redisUtil;
    @Mock
    private ObjectProvider<ITenantPermissionService> tenantPermissionServiceProvider;
    @Mock
    private ObjectProvider<IGrantPermissionService> grantPermissionServiceProvider;
    @Mock
    private ITenantPermissionService tenantPermissionService;
    @Mock
    private IGrantPermissionService grantPermissionService;

    private RedisPermissionCache cache;

    private static UrlPermission urlPermission(String name) {
        return new UrlPermission(name, name, MultiTenancySides.All);
    }

    @BeforeEach
    void setUp() {
        cache = new RedisPermissionCache();
        ReflectionTestUtils.setField(cache, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(cache, "tenantPermissionServiceProvider", tenantPermissionServiceProvider);
        ReflectionTestUtils.setField(cache, "grantPermissionServiceProvider", grantPermissionServiceProvider);
    }

    @Test
    @DisplayName("addAnonymousPath：保留已有应用并写回 Redis")
    void addAnonymousPathKeepsExisting() {
        Map<String, List<String>> existing = new HashMap<>();
        existing.put("other", List.of("/other"));
        when(redisUtil.getCache(ANONYMOUS_KEY)).thenReturn(existing);

        cache.addAnonymousPath("app", List.of("/login"));

        assertThat(existing).containsEntry("app", List.of("/login"));
        verify(redisUtil).setCache(ANONYMOUS_KEY, existing);
    }

    @Test
    @DisplayName("addAnonymousPath：缓存为空时新建 Map")
    void addAnonymousPathWhenCacheEmpty() {
        when(redisUtil.getCache(ANONYMOUS_KEY)).thenReturn(null);

        cache.addAnonymousPath("app", List.of("/login"));

        verify(redisUtil).setCache(eq(ANONYMOUS_KEY), any());
    }

    @Test
    @DisplayName("getAllowAnonymousPath：命中返回缓存，未命中返回空 Map")
    void getAllowAnonymousPath() {
        Map<String, List<String>> cached = Map.of("app", List.of("/login"));
        when(redisUtil.getCache(ANONYMOUS_KEY)).thenReturn(cached);

        assertThat(cache.getAllowAnonymousPath()).isSameAs(cached);

        when(redisUtil.getCache(ANONYMOUS_KEY)).thenReturn(null);
        assertThat(cache.getAllowAnonymousPath()).isEmpty();
    }

    @Test
    @DisplayName("addDefinitionPermission：合并已有定义并写回 Redis")
    void addDefinitionPermission() {
        Map<String, Map<String, Permission>> existing = new HashMap<>();
        Map<String, Permission> permissionMap = Map.of("p", new Permission("p", "p", MultiTenancySides.All));
        when(redisUtil.getCache(DEFINITION_KEY)).thenReturn(existing);

        cache.addDefinitionPermission("app", permissionMap);

        assertThat(existing).containsEntry("app", permissionMap);
        verify(redisUtil).setCache(DEFINITION_KEY, existing);
    }

    @Test
    @DisplayName("getDefinitionPermission：命中返回缓存，未命中返回空 Map")
    void getDefinitionPermission() {
        Map<String, Map<String, Permission>> cached = Map.of("app", Map.of());
        when(redisUtil.getCache(DEFINITION_KEY)).thenReturn(cached);

        assertThat(cache.getDefinitionPermission()).isSameAs(cached);

        when(redisUtil.getCache(DEFINITION_KEY)).thenReturn(null);
        assertThat(cache.getDefinitionPermission()).isEmpty();
    }

    @Test
    @DisplayName("getUrlPermission：存在旧 key 时先删除，再返回新缓存")
    void getUrlPermissionDeletesLegacyKey() {
        Map<String, Map<String, List<UrlPermission>>> cached = Map.of("app", Map.of());
        when(redisUtil.hasKey(URL_KEY_OLD)).thenReturn(true);
        when(redisUtil.getCache(URL_KEY)).thenReturn(cached);

        assertThat(cache.getUrlPermission()).isSameAs(cached);
        verify(redisUtil).deleteCache(URL_KEY_OLD);
    }

    @Test
    @DisplayName("getUrlPermission：无旧 key 且无缓存时返回空 Map")
    void getUrlPermissionWhenEmpty() {
        when(redisUtil.hasKey(URL_KEY_OLD)).thenReturn(false);
        when(redisUtil.getCache(URL_KEY)).thenReturn(null);

        assertThat(cache.getUrlPermission()).isEmpty();
        verify(redisUtil, never()).deleteCache(URL_KEY_OLD);
    }

    @Test
    @DisplayName("addUrlPermission：写入后刷新角色并回写 Redis")
    void addUrlPermissionRefreshes() {
        Map<String, Map<String, List<UrlPermission>>> urlPermissions = new HashMap<>();
        when(redisUtil.hasKey(URL_KEY_OLD)).thenReturn(false);
        when(redisUtil.getCache(URL_KEY)).thenReturn(urlPermissions);
        when(grantPermissionServiceProvider.getIfAvailable()).thenReturn(grantPermissionService);
        when(tenantPermissionServiceProvider.getIfAvailable()).thenReturn(tenantPermissionService);
        when(grantPermissionService.getAll()).thenReturn(Map.of());
        when(tenantPermissionService.getAllTenantPermission()).thenReturn(Map.of());

        cache.addUrlPermission("app", Map.of("/api/x", List.of(urlPermission("p"))));

        assertThat(urlPermissions).containsKey("app");
        verify(redisUtil, times(2)).setCache(eq(URL_KEY), any());
    }

    @Test
    @DisplayName("refreshAll：授权服务不可用时直接返回")
    void refreshAllSkipsWhenGrantServiceMissing() {
        when(grantPermissionServiceProvider.getIfAvailable()).thenReturn(null);

        cache.refreshAll();

        verify(redisUtil, never()).setCache(eq(URL_KEY), any());
    }

    @Test
    @DisplayName("refreshAll：租户服务不可用时只刷新宿主授权")
    void refreshAllWithoutTenantService() {
        when(grantPermissionServiceProvider.getIfAvailable()).thenReturn(grantPermissionService);
        when(tenantPermissionServiceProvider.getIfAvailable()).thenReturn(null);
        when(grantPermissionService.getAll()).thenReturn(Map.of());
        when(redisUtil.hasKey(URL_KEY_OLD)).thenReturn(false);
        when(redisUtil.getCache(URL_KEY)).thenReturn(null);

        cache.refreshAll();

        verify(redisUtil).setCache(eq(URL_KEY), any());
    }

    @Test
    @DisplayName("refreshAll：合并宿主与租户角色后回写缓存")
    void refreshAllMergesRoles() {
        UrlPermission target = urlPermission("p");
        Map<String, List<UrlPermission>> applicationPermission = new HashMap<>();
        applicationPermission.put("/api/x", List.of(target));
        Map<String, Map<String, List<UrlPermission>>> urlPermissions = new HashMap<>();
        urlPermissions.put("app", applicationPermission);

        RoleInfo hostRole = new RoleInfo();
        hostRole.setRoleCode("host");
        RoleInfo tenantRole = new RoleInfo();
        tenantRole.setRoleCode("tenant");

        when(grantPermissionServiceProvider.getIfAvailable()).thenReturn(grantPermissionService);
        when(tenantPermissionServiceProvider.getIfAvailable()).thenReturn(tenantPermissionService);
        when(grantPermissionService.getAll()).thenReturn(Map.of(
                "p", List.of(hostRole),
                "1p", List.of(tenantRole)));
        when(tenantPermissionService.getAllTenantPermission()).thenReturn(Map.of("p", List.of(1L)));
        when(redisUtil.hasKey(URL_KEY_OLD)).thenReturn(false);
        when(redisUtil.getCache(URL_KEY)).thenReturn(urlPermissions);

        cache.refreshAll();

        assertThat(target.getRoles()).extracting(RoleInfo::getRoleCode)
                .containsExactly("host", "tenant");
        verify(redisUtil).setCache(URL_KEY, urlPermissions);
    }
}
