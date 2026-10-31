package com.dusk.module.auth.service.impl;

import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.core.utils.SpringContextUtils;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.IStationService;
import com.dusk.module.auth.service.ITenantService;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link StationMigrationService} 的 {@code @PostConstruct} 入口补充测试：
 * 覆盖通过 {@link SpringContextUtils} 取自身代理后按需迁移的两个分支。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StationMigrationServiceCoverageTest {

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
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("autoMigration：厂站表为空时执行迁移并刷新数据过滤器")
    void autoMigration_runsWhenNeeded() {
        when(stationService.count()).thenReturn(0L);
        when(tenantService.findAll()).thenReturn(List.of());

        try (MockedStatic<SpringContextUtils> context = mockStatic(SpringContextUtils.class)) {
            context.when(() -> SpringContextUtils.getBean(StationMigrationService.class)).thenReturn(service);

            service.autoMigration();
        }

        verify(dataFilterDefinitionContext).refresh();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    @DisplayName("autoMigration：已有厂站数据时跳过迁移")
    void autoMigration_skipsWhenNotNeeded() {
        when(stationService.count()).thenReturn(1L);

        try (MockedStatic<SpringContextUtils> context = mockStatic(SpringContextUtils.class)) {
            context.when(() -> SpringContextUtils.getBean(StationMigrationService.class)).thenReturn(service);

            service.autoMigration();
        }

        verify(dataFilterDefinitionContext, never()).refresh();
        verifyNoInteractions(tenantService);
        verify(stationService, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
