package com.dusk.module.auth.service.impl;

import com.dusk.module.auth.entity.ExtendField;
import com.dusk.module.auth.entity.QExtendField;
import com.dusk.module.auth.entity.QStation;
import com.dusk.module.auth.repository.IExtendFieldRepository;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ExtendFieldServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class ExtendFieldServiceImplTest {

    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private IExtendFieldRepository repository;

    private ExtendFieldServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExtendFieldServiceImpl();
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "repository", repository);
    }

    @SuppressWarnings("rawtypes")
    private JPAQuery stubSelectFromChain() {
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.where(any(), any(), any())).thenReturn(query);
        return query;
    }

    @SuppressWarnings("rawtypes")
    private JPAQuery stubSelectChain() {
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.from(any(EntityPath.class))).thenReturn(query);
        return query;
    }

    @Test
    @DisplayName("addOrUpdateField：查询无结果时新建记录")
    void addOrUpdateFieldCreatesRecordWhenMissing() {
        // fetchFirst() 默认返回 null，即“查询无结果”分支
        stubSelectFromChain();

        service.addOrUpdateField(1L, "Cls", "key", "value");

        ArgumentCaptor<ExtendField> captor = ArgumentCaptor.forClass(ExtendField.class);
        verify(repository).save(captor.capture());
        ExtendField saved = captor.getValue();
        assertThat(saved.getEntityId()).isEqualTo(1L);
        assertThat(saved.getEntityClass()).isEqualTo("Cls");
        assertThat(saved.getKey()).isEqualTo("key");
        assertThat(saved.getValue()).isEqualTo("value");
    }

    @Test
    @DisplayName("addOrUpdateField：查询有结果时更新已有记录")
    void addOrUpdateFieldUpdatesExistingRecord() {
        ExtendField existing = new ExtendField();
        existing.setValue("old");

        @SuppressWarnings("rawtypes")
        JPAQuery query = stubSelectFromChain();
        when(query.fetchFirst()).thenReturn(existing);

        service.addOrUpdateField(1L, "Cls", "key", "new");

        ArgumentCaptor<ExtendField> captor = ArgumentCaptor.forClass(ExtendField.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(existing.getValue()).isEqualTo("new");
    }

    @Test
    @DisplayName("getValue：返回查询到的扩展字段值")
    void getValueReturnsStoredValue() {
        @SuppressWarnings("rawtypes")
        JPAQuery query = stubSelectChain();
        when(query.where(any(), any(), any())).thenReturn(query);
        when(query.fetchFirst()).thenReturn("stored");

        assertThat(service.getValue(1L, "Cls", "key")).isEqualTo("stored");
    }

    @Test
    @DisplayName("getEntityId：返回关联实体的主键")
    void getEntityIdReturnsJoinedId() {
        @SuppressWarnings("rawtypes")
        JPAQuery query = stubSelectChain();
        when(query.innerJoin(any(EntityPath.class))).thenReturn(query);
        when(query.on(any(Predicate.class))).thenReturn(query);
        when(query.where(any(), any(), any())).thenReturn(query);
        when(query.fetchFirst()).thenReturn(42L);

        assertThat(service.getEntityId("Station", QStation.station, "key", "value")).isEqualTo(42L);
    }

    @Test
    @DisplayName("getEntityIds：返回关联实体的主键列表")
    void getEntityIdsReturnsJoinedIds() {
        @SuppressWarnings("rawtypes")
        JPAQuery query = stubSelectChain();
        when(query.innerJoin(any(EntityPath.class))).thenReturn(query);
        when(query.on(any(Predicate.class))).thenReturn(query);
        when(query.where(any(), any(), any())).thenReturn(query);
        when(query.fetch()).thenReturn(List.of(1L, 2L));

        assertThat(service.getEntityIds("Station", QStation.station, "key", List.of("a", "b")))
                .containsExactly(1L, 2L);
    }
}
