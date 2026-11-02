package com.dusk.module.auth.common.datafilter.impl;

import com.dusk.common.core.constant.EntityConstant;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.repository.IStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link DataFilterDefinitionContext} 单元测试：覆盖组织树初始化（含递归子节点）、
 * 空数据、refresh 重建以及父节点不存在时的防御分支。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataFilterDefinitionContextTest {

    @Mock
    private IStationRepository stationRepository;

    private DataFilterDefinitionContext context;

    private static Station station(Long id, Long parentId) {
        Station station = new Station();
        station.setId(id);
        station.setParentId(parentId);
        return station;
    }

    @BeforeEach
    void setUp() {
        context = new DataFilterDefinitionContext();
        ReflectionTestUtils.setField(context, "stationRepository", stationRepository);
    }

    private void mockTree() {
        when(stationRepository.findAllByDr(EntityConstant.LOGIC_UN_DELETE_VALUE)).thenReturn(List.of(
                station(1L, null),
                station(2L, 1L),
                station(3L, 1L),
                station(4L, 2L)));
    }

    @Test
    @DisplayName("initialize：根节点递归收集所有后代厂站")
    void initializeCollectsDescendants() {
        mockTree();

        context.initialize();

        Map<String, List<Long>> definition = context.getDataFilterDefinition();
        assertThat(definition.get("1")).containsExactly(1L, 2L, 4L, 3L);
        assertThat(definition.get("2")).containsExactly(2L, 4L);
        assertThat(definition.get("3")).containsExactly(3L);
        assertThat(definition.get("4")).containsExactly(4L);
    }

    @Test
    @DisplayName("initialize：没有厂站时得到空映射")
    void initializeWithNoStations() {
        when(stationRepository.findAllByDr(EntityConstant.LOGIC_UN_DELETE_VALUE)).thenReturn(List.of());

        context.initialize();

        assertThat(context.getDataFilterDefinition()).isEmpty();
    }

    @Test
    @DisplayName("refresh：清空旧数据并按最新厂站重建")
    void refreshRebuilds() {
        mockTree();
        context.initialize();
        when(stationRepository.findAllByDr(EntityConstant.LOGIC_UN_DELETE_VALUE))
                .thenReturn(List.of(station(9L, null)));

        context.refresh();

        assertThat(context.getDataFilterDefinition()).containsOnlyKeys("9");
        assertThat(context.getDataFilterDefinition().get("9")).containsExactly(9L);
        verify(stationRepository, times(2)).findAllByDr(EntityConstant.LOGIC_UN_DELETE_VALUE);
    }

    @Test
    @DisplayName("父节点不在映射中时不写入子节点，也不抛异常")
    void ignoresChildrenWhenParentMissing() throws Exception {
        java.lang.reflect.Method method = DataFilterDefinitionContext.class.getDeclaredMethod(
                "addAllChildren", List.class, List.class, Long.class);
        method.setAccessible(true);

        method.invoke(context, List.of(station(2L, 1L)), List.of(station(2L, 1L)), 1L);

        assertThat(context.getDataFilterDefinition()).isEmpty();
    }
}
