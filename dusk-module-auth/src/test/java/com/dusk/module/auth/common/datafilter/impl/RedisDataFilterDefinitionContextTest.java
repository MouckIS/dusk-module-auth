package com.dusk.module.auth.common.datafilter.impl;

import com.dusk.common.core.constant.EntityConstant;
import com.dusk.common.core.redis.RedisUtil;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.repository.IStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link RedisDataFilterDefinitionContext} 单元测试：覆盖组织树写入 Redis、缓存命中/未命中、
 * refresh 重建以及父节点不存在时的防御分支。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisDataFilterDefinitionContextTest {

    private static final String REDIS_KEY = "CRUX:AUTH:DATA:FILTER:DEFINITION";

    @Mock
    private IStationRepository stationRepository;
    @Mock
    private RedisUtil<Object> redisUtil;

    private RedisDataFilterDefinitionContext context;

    private static Station station(Long id, Long parentId) {
        Station station = new Station();
        station.setId(id);
        station.setParentId(parentId);
        return station;
    }

    @BeforeEach
    void setUp() {
        context = new RedisDataFilterDefinitionContext();
        ReflectionTestUtils.setField(context, "stationRepository", stationRepository);
        ReflectionTestUtils.setField(context, "redisUtil", redisUtil);
    }

    private void mockTree() {
        when(stationRepository.findAllByDr(EntityConstant.LOGIC_UN_DELETE_VALUE)).thenReturn(List.of(
                station(1L, null),
                station(2L, 1L),
                station(3L, 1L),
                station(4L, 2L)));
    }

    @Test
    @DisplayName("initialize：构建组织树并写入 Redis")
    void initializeWritesTreeToRedis() {
        mockTree();

        context.initialize();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(redisUtil).setCache(eq(REDIS_KEY), captor.capture());
        Map<String, List<Long>> stored = (Map<String, List<Long>>) captor.getValue();
        assertThat(stored.get("1")).containsExactly(1L, 2L, 4L, 3L);
        assertThat(stored.get("2")).containsExactly(2L, 4L);
        assertThat(stored.get("3")).containsExactly(3L);
        assertThat(stored.get("4")).containsExactly(4L);
    }

    @Test
    @DisplayName("initialize：没有厂站时写入空映射")
    void initializeWithNoStations() {
        when(stationRepository.findAllByDr(EntityConstant.LOGIC_UN_DELETE_VALUE)).thenReturn(List.of());

        context.initialize();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(redisUtil).setCache(eq(REDIS_KEY), captor.capture());
        assertThat((Map<String, List<Long>>) captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("getDataFilterDefinition：命中返回缓存，未命中返回空 Map")
    void getDataFilterDefinition() {
        Map<String, List<Long>> cached = Map.of("1", List.of(1L));
        when(redisUtil.getCache(REDIS_KEY)).thenReturn(cached);

        assertThat(context.getDataFilterDefinition()).isSameAs(cached);

        when(redisUtil.getCache(REDIS_KEY)).thenReturn(null);
        assertThat(context.getDataFilterDefinition()).isEmpty();
    }

    @Test
    @DisplayName("refresh：重建组织树并再次写入 Redis")
    void refreshRebuilds() {
        mockTree();
        context.initialize();
        when(stationRepository.findAllByDr(EntityConstant.LOGIC_UN_DELETE_VALUE))
                .thenReturn(List.of(station(9L, null)));

        context.refresh();

        verify(redisUtil, times(2)).setCache(eq(REDIS_KEY), org.mockito.ArgumentMatchers.any());
        verify(stationRepository, times(2)).findAllByDr(EntityConstant.LOGIC_UN_DELETE_VALUE);
    }

    @Test
    @DisplayName("父节点不在映射中时不写入子节点，也不抛异常")
    void ignoresChildrenWhenParentMissing() throws Exception {
        Map<String, List<Long>> definition = new HashMap<>();
        java.lang.reflect.Method method = RedisDataFilterDefinitionContext.class.getDeclaredMethod(
                "addAllChildren", List.class, List.class, Long.class, Map.class);
        method.setAccessible(true);

        method.invoke(context, List.of(station(2L, 1L)), List.of(station(2L, 1L)), 1L, definition);

        assertThat(definition).isEmpty();
    }
}
