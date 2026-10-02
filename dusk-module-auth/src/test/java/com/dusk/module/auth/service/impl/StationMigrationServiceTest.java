package com.dusk.module.auth.service.impl;

import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.IStationService;
import com.dusk.module.auth.service.ITenantService;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link StationMigrationService} 单元测试，目标：覆盖 service.impl 门禁包的旧组织机构 → 厂站迁移逻辑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StationMigrationServiceTest {

    @Mock
    private ITenantService tenantService;
    @Mock
    private IOrganizationUnitService organizationUnitService;
    @Mock
    private IStationService stationService;
    @Mock
    private IDataFilterDefinitionContext dataFilterDefinitionContext;
    @Mock
    private JPAQueryFactory queryFactory;

    private StationMigrationService service;

    @BeforeEach
    void setUp() {
        service = new StationMigrationService();
        ReflectionTestUtils.setField(service, "tenantService", tenantService);
        ReflectionTestUtils.setField(service, "organizationUnitService", organizationUnitService);
        ReflectionTestUtils.setField(service, "stationService", stationService);
        ReflectionTestUtils.setField(service, "dataFilterDefinitionContext", dataFilterDefinitionContext);
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        when(stationService.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static OrganizationUnit unit(Long id, Long parentId, boolean station) {
        OrganizationUnit u = new OrganizationUnit();
        u.setId(id);
        u.setParentId(parentId);
        u.setDisplayName("org" + id);
        u.setPath(parentId == null ? String.valueOf(id) : parentId + "/" + id);
        u.setSortIndex(1);
        u.setStation(station);
        u.setUsers(new ArrayList<>());
        return u;
    }

    private static Tenant tenant(Long id, String name) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setName(name);
        return t;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubOrgUsersQuery() {
        JPAQuery query = org.mockito.Mockito.mock(JPAQuery.class);
        when(query.from(any(com.querydsl.core.types.EntityPath.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.fetch()).thenReturn(List.of(new User()));
    }

    // ------------------------------------------------------------------
    // needMigration
    // ------------------------------------------------------------------

    @Test
    @DisplayName("needMigration：厂站表为空时需要迁移")
    void needMigration_true() {
        when(stationService.count()).thenReturn(0L);

        assertThat(service.needMigration()).isTrue();
    }

    @Test
    @DisplayName("needMigration：已有厂站数据时无需迁移")
    void needMigration_false() {
        when(stationService.count()).thenReturn(3L);

        assertThat(service.needMigration()).isFalse();
    }

    // ------------------------------------------------------------------
    // migration
    // ------------------------------------------------------------------

    @Test
    @DisplayName("migration：无租户时仅刷新数据过滤器")
    void migration_noTenants() {
        when(tenantService.findAll()).thenReturn(List.of());

        service.migration();

        verify(dataFilterDefinitionContext).refresh();
    }

    @Test
    @DisplayName("migration：按租户逐个同步组织机构为厂站")
    void migration_syncsStations() {
        when(tenantService.findAll()).thenReturn(List.of(tenant(1L, "t1")));
        OrganizationUnit rootStation = unit(1L, null, true);
        OrganizationUnit rootPlain = unit(10L, null, false);
        OrganizationUnit childPlain = unit(2L, 1L, false);
        OrganizationUnit childStation = unit(3L, 1L, true);
        OrganizationUnit leafStation = unit(11L, 10L, true);
        when(organizationUnitService.findAll(any(Sort.class)))
                .thenReturn(List.of(rootStation, rootPlain, childPlain, childStation, leafStation));
        when(stationService.findById(anyLong())).thenReturn(Optional.empty());
        stubOrgUsersQuery();

        service.migration();

        verify(stationService, atLeastOnce()).save(any(Station.class));
        verify(dataFilterDefinitionContext).refresh();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    @DisplayName("migration：厂站已存在时不重复创建")
    void migration_stationAlreadyExists() {
        when(tenantService.findAll()).thenReturn(List.of(tenant(1L, "t1")));
        when(organizationUnitService.findAll(any(Sort.class)))
                .thenReturn(List.of(unit(1L, null, true)));
        when(stationService.findById(1L)).thenReturn(Optional.of(new Station()));

        service.migration();

        verify(stationService, org.mockito.Mockito.never()).save(any(Station.class));
    }

    @Test
    @DisplayName("migration：单个租户同步异常被吞掉且不影响后续租户")
    void migration_swallowsException() {
        when(tenantService.findAll()).thenReturn(List.of(tenant(1L, "t1"), tenant(2L, "t2")));
        when(organizationUnitService.findAll(any(Sort.class)))
                .thenThrow(new IllegalStateException("boom"));

        service.migration();

        verify(dataFilterDefinitionContext).refresh();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    @DisplayName("migration：根节点为非厂站时仍需递归其子机构")
    void migration_plainRootWithStationChild() {
        when(tenantService.findAll()).thenReturn(List.of(tenant(1L, "t1")));
        when(organizationUnitService.findAll(any(Sort.class)))
                .thenReturn(List.of(unit(1L, null, false), unit(2L, 1L, true)));
        when(stationService.findById(anyLong())).thenReturn(Optional.empty());
        stubOrgUsersQuery();

        service.migration();

        verify(stationService).save(any(Station.class));
    }

    @Test
    @DisplayName("migration：多级厂站嵌套时父厂站作为子厂站 parentId")
    void migration_nestedStations() {
        when(tenantService.findAll()).thenReturn(List.of(tenant(1L, "t1")));
        when(organizationUnitService.findAll(any(Sort.class)))
                .thenReturn(List.of(unit(1L, null, true), unit(2L, 1L, true)));
        when(stationService.findById(anyLong())).thenReturn(Optional.empty());
        stubOrgUsersQuery();

        service.migration();

        org.mockito.ArgumentCaptor<Station> captor = org.mockito.ArgumentCaptor.forClass(Station.class);
        verify(stationService, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        List<Station> saved = captor.getAllValues();
        Station childStation = saved.stream().filter(s -> Long.valueOf(2L).equals(s.getId())).findFirst().orElseThrow();
        assertThat(childStation.getParentId()).isEqualTo(1L);
    }
}
