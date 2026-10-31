package com.dusk.module.auth.common.filter;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.exception.TenantNotValidException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.core.utils.UserContextUtils;
import com.dusk.module.auth.dto.TenantInfoDto;
import com.dusk.module.auth.service.ITenantRpcService;
import jakarta.servlet.FilterChain;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link TenantContextHolderFilter} 单元测试，目标：>=95% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantContextHolderFilterTest {

    private final AtomicReference<Long> tenantInChain = new AtomicReference<>();
    private final AtomicReference<Long> userInChain = new AtomicReference<>();
    @Mock
    private ITenantRpcService tenantRpcService;
    @Mock
    private UserContextUtils userContextUtils;
    @Mock
    private FilterChain filterChain;
    private TenantContextHolderFilter filter;

    private static TenantInfoDto tenant(Long id, boolean enabled) {
        TenantInfoDto dto = new TenantInfoDto();
        dto.setId(id);
        dto.setEnabled(enabled);
        return dto;
    }

    private static MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/api/test");
    }

    @BeforeEach
    void setUp() throws Exception {
        filter = new TenantContextHolderFilter();
        ReflectionTestUtils.setField(filter, "tenantRpcService", tenantRpcService);
        ReflectionTestUtils.setField(filter, "userContextUtils", userContextUtils);
        ReflectionTestUtils.setField(filter, "tenantDomainLevel", 4);
        doAnswer(invocation -> {
            tenantInChain.set(TenantContextHolder.getTenantId());
            userInChain.set(LoginUserIdContextHolder.getUserId());
            return null;
        }).when(filterChain).doFilter(any(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        LoginUserIdContextHolder.clear();
    }

    @Test
    @DisplayName("doFilter：已登录用户以 UserContext 中的租户与用户为准，并清理上下文")
    void doFilter_userContextPresent() throws Exception {
        UserContext userContext = new UserContext();
        userContext.setId(7L);
        userContext.setTenantId(9L);
        when(userContextUtils.getUserContext(any(jakarta.servlet.http.HttpServletRequest.class))).thenReturn(userContext);

        MockHttpServletRequest request = request();
        request.addHeader(TenantContextHolderFilter.TENANT_ID_HEADER, "999");
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isEqualTo(9L);
        assertThat(userInChain.get()).isEqualTo(7L);
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(LoginUserIdContextHolder.getUserId()).isNull();
        verify(tenantRpcService, org.mockito.Mockito.never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("doFilter：未登录时使用 TENANT-ID 请求头且租户可用")
    void doFilter_tenantIdHeader() throws Exception {
        when(tenantRpcService.findById(3L)).thenReturn(tenant(3L, true));
        MockHttpServletRequest request = request();
        request.addHeader(TenantContextHolderFilter.TENANT_ID_HEADER, "3");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isEqualTo(3L);
        assertThat(userInChain.get()).isNull();
        verify(tenantRpcService).findById(3L);
    }

    @Test
    @DisplayName("doFilter：TENANT-ID 缺失时兼容旧 TENANT_ID 请求头")
    void doFilter_oldTenantIdHeader() throws Exception {
        when(tenantRpcService.findById(4L)).thenReturn(tenant(4L, true));
        MockHttpServletRequest request = request();
        request.addHeader("TENANT_ID", "4");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isEqualTo(4L);
        verify(tenantRpcService).findById(4L);
    }

    @Test
    @DisplayName("doFilter：TENANT-ID 为空白时退化为按租户名解析")
    void doFilter_blankTenantIdFallbackToName() throws Exception {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(tenant(5L, true));
        MockHttpServletRequest request = request();
        request.addHeader(TenantContextHolderFilter.TENANT_ID_HEADER, "   ");
        request.addHeader("TENANT-NAME", "acme");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isEqualTo(5L);
        verify(tenantRpcService).findByTenantName("acme");
    }

    @Test
    @DisplayName("doFilter：TENANT-ID 对应租户不存在时抛出 TenantNotValidException")
    void doFilter_tenantIdNotFound() {
        when(tenantRpcService.findById(3L)).thenReturn(null);
        MockHttpServletRequest request = request();
        request.addHeader(TenantContextHolderFilter.TENANT_ID_HEADER, "3");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), filterChain))
                .isInstanceOf(TenantNotValidException.class)
                .hasMessage("租户不存在，请检查");
        assertThat(request.getAttribute("exception")).isInstanceOf(TenantNotValidException.class);
    }

    @Test
    @DisplayName("doFilter：TENANT-ID 对应租户被禁用时抛出 TenantNotValidException")
    void doFilter_tenantIdDisabled() {
        when(tenantRpcService.findById(3L)).thenReturn(tenant(3L, false));
        MockHttpServletRequest request = request();
        request.addHeader(TenantContextHolderFilter.TENANT_ID_HEADER, "3");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), filterChain))
                .isInstanceOf(TenantNotValidException.class);
    }

    @Test
    @DisplayName("doFilter：按 TENANT-NAME 解析到可用租户")
    void doFilter_tenantNameHeader() throws Exception {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(tenant(6L, true));
        MockHttpServletRequest request = request();
        request.addHeader("TENANT-NAME", "acme");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isEqualTo(6L);
    }

    @Test
    @DisplayName("doFilter：TENANT-NAME 缺失时兼容旧 TENANT_NAME 请求头")
    void doFilter_oldTenantNameHeader() throws Exception {
        when(tenantRpcService.findByTenantName("legacy")).thenReturn(tenant(8L, true));
        MockHttpServletRequest request = request();
        request.addHeader("TENANT_NAME", "legacy");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isEqualTo(8L);
        verify(tenantRpcService).findByTenantName("legacy");
    }

    @Test
    @DisplayName("doFilter：租户名不存在时抛出 TenantNotValidException")
    void doFilter_tenantNameNotFound() {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(null);
        MockHttpServletRequest request = request();
        request.addHeader("TENANT-NAME", "acme");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), filterChain))
                .isInstanceOf(TenantNotValidException.class);
    }

    @Test
    @DisplayName("doFilter：租户名对应租户被禁用时抛出 TenantNotValidException")
    void doFilter_tenantNameDisabled() {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(tenant(6L, false));
        MockHttpServletRequest request = request();
        request.addHeader("TENANT-NAME", "acme");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), filterChain))
                .isInstanceOf(TenantNotValidException.class);
    }

    @Test
    @DisplayName("getTenantBySubDomain：x-forwarded-host 多级代理取首个域名")
    void subDomain_fromForwardedHostWithProxyChain() throws Exception {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(tenant(11L, true));
        MockHttpServletRequest request = request();
        request.addHeader("x-forwarded-host", "acme.dusk.com.cn, proxy.local");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isEqualTo(11L);
        verify(tenantRpcService).findByTenantName("acme");
    }

    @Test
    @DisplayName("getTenantBySubDomain：x-forwarded-host 去掉 www. 前缀与端口")
    void subDomain_stripWwwAndPort() throws Exception {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(tenant(12L, true));
        MockHttpServletRequest request = request();
        request.addHeader("x-forwarded-host", "www.acme.dusk.com.cn:8080");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isEqualTo(12L);
        verify(tenantRpcService).findByTenantName("acme");
    }

    @Test
    @DisplayName("getTenantBySubDomain：无 x-forwarded-host 时回退 host 请求头")
    void subDomain_fromHostHeader() throws Exception {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(tenant(13L, true));
        MockHttpServletRequest request = request();
        request.addHeader("host", "acme.dusk.com.cn");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isEqualTo(13L);
    }

    @Test
    @DisplayName("getTenantBySubDomain：host 带端口时去掉端口再匹配")
    void subDomain_hostWithPort() throws Exception {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(tenant(14L, true));
        MockHttpServletRequest request = request();
        request.addHeader("host", "acme.dusk.com.cn:8080");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isEqualTo(14L);
    }

    @Test
    @DisplayName("getTenantBySubDomain：IPv6 访问不识别租户")
    void subDomain_ipv6() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("host", "[::1]:8080");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isNull();
        verify(tenantRpcService, org.mockito.Mockito.never()).findByTenantName(any());
    }

    @Test
    @DisplayName("getTenantBySubDomain：IPv4 访问不识别租户")
    void subDomain_ipv4() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("host", "192.168.1.1");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isNull();
        verify(tenantRpcService, org.mockito.Mockito.never()).findByTenantName(any());
    }

    @Test
    @DisplayName("getTenantBySubDomain：域名层级不足时不识别租户")
    void subDomain_notEnoughLevels() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("host", "dusk.com");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isNull();
        verify(tenantRpcService, org.mockito.Mockito.never()).findByTenantName(any());
    }

    @Test
    @DisplayName("getTenantBySubDomain：域名层级足够但租户不存在时不识别租户")
    void subDomain_tenantNotFound() throws Exception {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(null);
        MockHttpServletRequest request = request();
        request.addHeader("host", "acme.dusk.com.cn");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isNull();
    }

    @Test
    @DisplayName("getTenantBySubDomain：域名匹配到被禁用的租户时不识别租户")
    void subDomain_tenantDisabled() throws Exception {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(tenant(15L, false));
        MockHttpServletRequest request = request();
        request.addHeader("host", "acme.dusk.com.cn");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isNull();
    }

    @Test
    @DisplayName("getTenantBySubDomain：无任何租户信息时不设置租户与用户")
    void subDomain_noHostHeader() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("host", "unknown.dusk.com.cn");
        when(tenantRpcService.findByTenantName("unknown")).thenReturn(null);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(tenantInChain.get()).isNull();
        assertThat(userInChain.get()).isNull();
    }

    @Test
    @DisplayName("getTenantBySubDomain：直接调用时 x-forwarded-host 为单域名（无逗号）")
    void getTenantBySubDomain_singleForwardedHost() {
        when(tenantRpcService.findByTenantName("acme")).thenReturn(tenant(16L, true));
        MockHttpServletRequest request = request();
        request.addHeader("x-forwarded-host", "acme.dusk.com.cn");

        Long tenantId = ReflectionTestUtils.invokeMethod(filter, "getTenantBySubDomain", request);

        assertThat(tenantId).isEqualTo(16L);
    }

    @Test
    @DisplayName("getTenantBySubDomain：直接调用时返回 null 表示无匹配租户")
    void getTenantBySubDomain_noMatch() {
        MockHttpServletRequest request = request();
        request.addHeader("host", "www.dusk.com");

        Long tenantId = ReflectionTestUtils.invokeMethod(filter, "getTenantBySubDomain", request);

        assertThat(tenantId).isNull();
    }
}
