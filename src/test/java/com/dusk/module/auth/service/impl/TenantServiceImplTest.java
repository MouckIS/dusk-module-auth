package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.dto.tenant.CreateTenantInput;
import com.dusk.module.auth.dto.tenant.GetTenantsInput;
import com.dusk.module.auth.dto.tenant.IsTenantAvailableInput;
import com.dusk.module.auth.dto.tenant.IsTenantAvailableOutput;
import com.dusk.module.auth.dto.tenant.TenantAvailabilityState;
import com.dusk.module.auth.dto.tenant.TenantEditDto;
import com.dusk.module.auth.entity.SubscribableEdition;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.ITenantRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ISubscribableEditionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TenantServiceImpl} 单元测试，目标：覆盖 service.impl 门禁包的租户生命周期逻辑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantServiceImplTest {

    @Mock
    private ITenantRepository repository;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ISubscribableEditionService editionService;
    @Mock
    private IAuthPermissionManager authPermissionManager;

    private TenantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new TenantServiceImpl());
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "iUserRepository", userRepository);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "editionService", editionService);
        ReflectionTestUtils.setField(service, "authPermissionManager", authPermissionManager);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static Tenant tenant(Long id, String tenantName, boolean active) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setTenantName(tenantName);
        t.setName("租户" + id);
        t.setActive(active);
        return t;
    }

    private static SubscribableEdition edition(Long id, BigDecimal monthly) {
        SubscribableEdition e = new SubscribableEdition();
        e.setId(id);
        e.setDisplayName("版本" + id);
        e.setMonthlyPrice(monthly);
        e.setAnnualPrice(monthly);
        return e;
    }

    // ------------------------------------------------------------------
    // createTenantWithDefaultSettings
    // ------------------------------------------------------------------

    @Test
    @DisplayName("createTenantWithDefaultSettings：名称唯一时创建租户与管理员")
    void createTenant() {
        when(repository.findByTenantName("t1")).thenReturn(Optional.empty());
        when(repository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(88L);
            return t;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        CreateTenantInput input = new CreateTenantInput();
        input.setTenantName("t1");
        input.setAdminUserName("admin");
        input.setAdminEmailAddress("a@b.c");
        input.setAdminPassword("pwd");

        Tenant result = service.createTenantWithDefaultSettings(input);

        assertThat(result.getId()).isEqualTo(88L);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(88L);
        assertThat(captor.getValue().isAdmin()).isTrue();
        verify(authPermissionManager).refreshAll();
    }

    @Test
    @DisplayName("createTenantWithDefaultSettings：未填管理员密码时生成随机密码并触发激活邮件分支")
    void createTenant_randomPasswordAndActivation() {
        when(repository.findByTenantName("t2")).thenReturn(Optional.empty());
        when(repository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(89L);
            return t;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        CreateTenantInput input = new CreateTenantInput();
        input.setTenantName("t2");
        input.setAdminUserName("admin2");
        input.setShouldChangePasswordOnNextLogin(true);
        input.setSendActivationEmail(true);

        service.createTenantWithDefaultSettings(input);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isShouldChangePasswordOnNextLogin()).isTrue();
    }

    @Test
    @DisplayName("createTenantWithDefaultSettings：租户代码重复抛业务异常")
    void createTenant_duplicateName() {
        when(repository.findByTenantName("dup")).thenReturn(Optional.of(tenant(1L, "dup", true)));
        CreateTenantInput input = new CreateTenantInput();
        input.setTenantName("dup");
        input.setName("重复");

        assertThatThrownBy(() -> service.createTenantWithDefaultSettings(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
        verify(repository, never()).save(any(Tenant.class));
    }

    @Test
    @DisplayName("createTenantWithDefaultSettings：指定版本时绑定版本信息")
    void createTenant_withEdition() {
        when(repository.findByTenantName("t3")).thenReturn(Optional.empty());
        when(editionService.findById(5L)).thenReturn(Optional.of(edition(5L, BigDecimal.TEN)));
        when(repository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(90L);
            return t;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        CreateTenantInput input = new CreateTenantInput();
        input.setTenantName("t3");
        input.setEditionId(5L);
        input.setAdminUserName("a");
        input.setAdminPassword("p");

        service.createTenantWithDefaultSettings(input);

        verify(repository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("createTenantWithDefaultSettings：版本不存在抛业务异常")
    void createTenant_editionNotFound() {
        when(repository.findByTenantName("t4")).thenReturn(Optional.empty());
        when(editionService.findById(6L)).thenReturn(Optional.empty());
        CreateTenantInput input = new CreateTenantInput();
        input.setTenantName("t4");
        input.setEditionId(6L);

        assertThatThrownBy(() -> service.createTenantWithDefaultSettings(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("版本");
    }

    // ------------------------------------------------------------------
    // updateTenant
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updateTenant：更新租户信息并刷新权限")
    void updateTenant() {
        when(repository.findById(1L)).thenReturn(Optional.of(tenant(1L, "t1", true)));
        when(repository.findByTenantName("t1")).thenReturn(Optional.of(tenant(1L, "t1", true)));
        TenantEditDto dto = new TenantEditDto();
        dto.setId(1L);
        dto.setTenantName("t1");
        dto.setName("新名");
        dto.setVersion(0);

        service.updateTenant(dto);

        verify(repository).save(any(Tenant.class));
        verify(authPermissionManager).refreshAll();
    }

    @Test
    @DisplayName("updateTenant：租户不存在抛业务异常")
    void updateTenant_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        TenantEditDto dto = new TenantEditDto();
        dto.setId(1L);

        assertThatThrownBy(() -> service.updateTenant(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("找不到");
    }

    @Test
    @DisplayName("updateTenant：名称被其它租户占用时抛异常")
    void updateTenant_nameConflict() {
        when(repository.findById(1L)).thenReturn(Optional.of(tenant(1L, "t1", true)));
        when(repository.findByTenantName("other")).thenReturn(Optional.of(tenant(2L, "other", true)));
        TenantEditDto dto = new TenantEditDto();
        dto.setId(1L);
        dto.setTenantName("other");
        dto.setName("冲突");
        dto.setVersion(0);

        assertThatThrownBy(() -> service.updateTenant(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("updateTenant：试用期内选择免费版本抛异常")
    void updateTenant_freeEditionInTrial() {
        when(repository.findById(1L)).thenReturn(Optional.of(tenant(1L, "t1", true)));
        when(repository.findByTenantName("t1")).thenReturn(Optional.of(tenant(1L, "t1", true)));
        when(editionService.findById(7L)).thenReturn(Optional.of(edition(7L, BigDecimal.ZERO)));
        TenantEditDto dto = new TenantEditDto();
        dto.setId(1L);
        dto.setTenantName("t1");
        dto.setEditionId(7L);
        dto.setInTrialPeriod(true);
        dto.setVersion(0);

        assertThatThrownBy(() -> service.updateTenant(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("免费版本");
    }

    @Test
    @DisplayName("updateTenant：试用期内选择付费版本正常更新")
    void updateTenant_paidEditionInTrial() {
        when(repository.findById(1L)).thenReturn(Optional.of(tenant(1L, "t1", true)));
        when(repository.findByTenantName("t1")).thenReturn(Optional.of(tenant(1L, "t1", true)));
        when(editionService.findById(8L)).thenReturn(Optional.of(edition(8L, BigDecimal.TEN)));
        TenantEditDto dto = new TenantEditDto();
        dto.setId(1L);
        dto.setTenantName("t1");
        dto.setEditionId(8L);
        dto.setInTrialPeriod(true);
        dto.setVersion(0);

        service.updateTenant(dto);

        verify(repository).save(any(Tenant.class));
    }

    // ------------------------------------------------------------------
    // 查询 / 可用性
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getTenants：按分页条件查询")
    void getTenants() {
        GetTenantsInput input = new GetTenantsInput();
        input.setFilter("abc");
        when(repository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tenant(1L, "t1", true))));

        Page<Tenant> result = service.getTenants(input);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getTenants：条件全空仍可查询")
    void getTenants_noFilter() {
        GetTenantsInput input = new GetTenantsInput();
        input.setEditionId("3");
        when(repository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.getTenants(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findByTenantName：委托仓储")
    void findByTenantName() {
        when(repository.findByTenantName("t1")).thenReturn(Optional.of(tenant(1L, "t1", true)));

        assertThat(service.findByTenantName("t1")).isPresent();
    }

    @Test
    @DisplayName("isTenantAvailable：租户不存在返回 NotFound")
    void isTenantAvailable_notFound() {
        when(repository.findByTenantName("none")).thenReturn(Optional.empty());
        IsTenantAvailableInput input = new IsTenantAvailableInput();
        input.setTenantName("none");

        IsTenantAvailableOutput output = service.isTenantAvailable(input);

        assertThat(output.state).isEqualTo(TenantAvailabilityState.NotFound);
    }

    @Test
    @DisplayName("isTenantAvailable：租户未启用返回 InActive")
    void isTenantAvailable_inactive() {
        when(repository.findByTenantName("off")).thenReturn(Optional.of(tenant(1L, "off", false)));
        IsTenantAvailableInput input = new IsTenantAvailableInput();
        input.setTenantName("off");

        assertThat(service.isTenantAvailable(input).state).isEqualTo(TenantAvailabilityState.InActive);
    }

    @Test
    @DisplayName("isTenantAvailable：租户启用返回 Available 且带 id")
    void isTenantAvailable_available() {
        Tenant t = tenant(9L, "on", true);
        t.setSubscriptionEndDateUtc(LocalDateTime.now().plusDays(1));
        when(repository.findByTenantName("on")).thenReturn(Optional.of(t));
        IsTenantAvailableInput input = new IsTenantAvailableInput();
        input.setTenantName("on");

        IsTenantAvailableOutput output = service.isTenantAvailable(input);

        assertThat(output.state).isEqualTo(TenantAvailabilityState.Available);
        assertThat(output.tenantId).isEqualTo(9L);
    }

    @Test
    @DisplayName("isTenantAvailable：订阅已过期视为不可用")
    void isTenantAvailable_expired() {
        Tenant t = tenant(9L, "expired", true);
        t.setSubscriptionEndDateUtc(LocalDateTime.now().minusDays(1));
        when(repository.findByTenantName("expired")).thenReturn(Optional.of(t));
        IsTenantAvailableInput input = new IsTenantAvailableInput();
        input.setTenantName("expired");

        assertThat(service.isTenantAvailable(input).state).isEqualTo(TenantAvailabilityState.InActive);
    }

    @Test
    @DisplayName("countTenantsByEdition：按版本统计租户数")
    void countTenantsByEdition() {
        when(repository.count(any(Specification.class))).thenReturn(3L);

        assertThat(service.countTenantsByEdition(5L)).isEqualTo(3L);
    }

    @Test
    @DisplayName("deleteTenant：删除后刷新权限")
    void deleteTenant() {
        service.deleteTenant(4L);

        verify(repository).deleteById(anyLong());
        verify(authPermissionManager).refreshAll();
    }
}
