package com.dusk.module.auth.service.impl;

import com.dusk.common.core.utils.SpringContextUtils;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.service.IStationService;
import com.dusk.module.auth.service.ITenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * {@link StationMigrationService#autoMigration()} 启动迁移分支补测。
 *
 * <p>使用 mockStatic 隔离 {@link SpringContextUtils}，不启动 Spring 容器。</p>
 */
class StationMigrationServiceAutoMigrationTest {

    private ITenantService tenantService;
    private IStationService stationService;
    private IDataFilterDefinitionContext dataFilterDefinitionContext;
    private StationMigrationService service;

    @BeforeEach
    void setUp() {
        tenantService = mock(ITenantService.class);
        stationService = mock(IStationService.class);
        dataFilterDefinitionContext = mock(IDataFilterDefinitionContext.class);
        service = new StationMigrationService();
        ReflectionTestUtils.setField(service, "tenantService", tenantService);
        ReflectionTestUtils.setField(service, "stationService", stationService);
        ReflectionTestUtils.setField(service, "dataFilterDefinitionContext", dataFilterDefinitionContext);
        when(tenantService.findAll()).thenReturn(List.of());
    }

    @Test
    @DisplayName("autoMigration：厂站表为空时执行迁移并刷新数据过滤器")
    void autoMigration_runsWhenNeeded() {
        when(stationService.count()).thenReturn(0L);

        try (MockedStatic<SpringContextUtils> mocked = mockStatic(SpringContextUtils.class)) {
            mocked.when(() -> SpringContextUtils.getBean(StationMigrationService.class)).thenReturn(service);

            service.autoMigration();
        }

        verify(dataFilterDefinitionContext).refresh();
    }

    @Test
    @DisplayName("autoMigration：已有厂站数据时跳过迁移")
    void autoMigration_skipsWhenNotNeeded() {
        when(stationService.count()).thenReturn(3L);

        try (MockedStatic<SpringContextUtils> mocked = mockStatic(SpringContextUtils.class)) {
            mocked.when(() -> SpringContextUtils.getBean(StationMigrationService.class)).thenReturn(service);

            service.autoMigration();
        }

        verify(dataFilterDefinitionContext, never()).refresh();
    }
}
