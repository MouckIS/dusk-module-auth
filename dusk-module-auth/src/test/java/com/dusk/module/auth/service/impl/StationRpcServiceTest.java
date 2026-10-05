package com.dusk.module.auth.service.impl;

import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.module.auth.dto.station.StationDto;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.repository.IStationRepository;
import com.dusk.module.auth.service.IStationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link StationRpcService} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class StationRpcServiceTest {

    @Mock
    private IStationService stationService;
    @Mock
    private IStationRepository stationRepository;

    private StationRpcService service;

    @BeforeEach
    void setUp() {
        service = new StationRpcService();
        ReflectionTestUtils.setField(service, "stationService", stationService);
        ReflectionTestUtils.setField(service, "stationRepository", stationRepository);
    }

    @AfterEach
    void tearDown() {
        DataFilterContextHolder.clear();
    }

    private static Station station(long id, String displayName) {
        Station station = new Station();
        station.setId(id);
        station.setDisplayName(displayName);
        return station;
    }

    @Test
    @DisplayName("getAllStations：直接透传服务层结果")
    void getAllStationsDelegates() {
        List<StationDto> expected = List.of(new StationDto());
        when(stationService.getAllStations()).thenReturn(expected);

        assertThat(service.getAllStations()).isSameAs(expected);
    }

    @Test
    @DisplayName("findOneByDisplayName：直接透传服务层结果")
    void findOneByDisplayNameDelegates() {
        StationDto dto = new StationDto();
        when(stationService.findOneByDisplayName("A")).thenReturn(dto);

        assertThat(service.findOneByDisplayName("A")).isSameAs(dto);
    }

    @Test
    @DisplayName("findOneById：厂站存在时映射为 DTO")
    void findOneByIdMapsWhenPresent() {
        when(stationService.findById(1L)).thenReturn(Optional.of(station(1L, "A")));

        assertThat(service.findOneById(1L)).isNotNull();
    }

    @Test
    @DisplayName("findOneById：厂站不存在时返回 null")
    void findOneByIdReturnsNullWhenAbsent() {
        when(stationService.findById(1L)).thenReturn(Optional.empty());

        assertThat(service.findOneById(1L)).isNull();
    }

    @Test
    @DisplayName("findByIds：ids 为 null 时返回空列表")
    void findByIdsReturnsEmptyWhenIdsNull() {
        assertThat(service.findByIds(null)).isEmpty();
    }

    @Test
    @DisplayName("findByIds：按 id 集合查询并映射")
    void findByIdsMapsResults() {
        when(stationService.findAll(any(Specification.class)))
                .thenReturn(List.of(station(1L, "A"), station(2L, "B")));

        assertThat(service.findByIds(List.of(1L, 2L))).hasSize(2);
    }

    @Test
    @DisplayName("getStationsByUserId：查询并映射用户可见厂站")
    void getStationsByUserIdMapsResults() {
        when(stationRepository.getStationsByUser(9L)).thenReturn(List.of(station(1L, "A")));

        assertThat(service.getStationsByUserId(9L)).hasSize(1);
    }

    @Test
    @DisplayName("getCurrentStation：未选择厂站时返回 null")
    void getCurrentStationReturnsNullWithoutDefaultOrg() {
        assertThat(service.getCurrentStation()).isNull();
    }

    @Test
    @DisplayName("getCurrentStation：已选择厂站时返回映射结果")
    void getCurrentStationMapsWhenSelected() {
        DataFilterContextHolder.setDataFilterId("5");
        when(stationService.findById(5L)).thenReturn(Optional.of(station(5L, "A")));

        assertThat(service.getCurrentStation()).isNotNull();
    }

    @Test
    @DisplayName("getCurrentStation：厂站已被删除时返回 null")
    void getCurrentStationReturnsNullWhenStationMissing() {
        DataFilterContextHolder.setDataFilterId("5");
        when(stationService.findById(5L)).thenReturn(Optional.empty());

        assertThat(service.getCurrentStation()).isNull();
    }

    @Test
    @DisplayName("getStationsByParentId：查询并映射下级厂站")
    void getStationsByParentIdMapsResults() {
        when(stationService.findDescendants(1L)).thenReturn(Collections.emptyList());

        assertThat(service.getStationsByParentId(1L)).isEmpty();
    }
}
