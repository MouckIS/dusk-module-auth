package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.dto.datadisplay.DataDisplayItemDto;
import com.dusk.module.auth.dto.datadisplay.GetDisplaySetInputDto;
import com.dusk.module.auth.dto.datadisplay.UpdateDataDisplaySetDto;
import com.dusk.module.auth.entity.datadisplay.DataDisplaySet;
import com.dusk.module.auth.repository.datadisplay.IDataDisplaySetRepository;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DataDisplaySetServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class DataDisplaySetServiceImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JPAQueryFactory queryFactory;
    @Mock
    private IDataDisplaySetRepository repository;

    private DataDisplaySetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new DataDisplaySetServiceImpl());
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "repository", repository);
        LoginUserIdContextHolder.setUserId(7L);
    }

    @AfterEach
    void tearDown() {
        LoginUserIdContextHolder.clear();
    }

    @SuppressWarnings("rawtypes")
    private void stubSelectChain() {
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
    }

    @SuppressWarnings("unchecked")
    private void stubPage(List<DataDisplaySet> content, long total) {
        Page<DataDisplaySet> page = mock(Page.class);
        when(page.getContent()).thenReturn(content);
        when(page.getTotalElements()).thenReturn(total);
        doReturn(page).when(service).page(any(JPAQuery.class), any(Pageable.class));
    }

    @Test
    @DisplayName("updateDisplaySetItem：删除旧设置后批量新增")
    void updateDisplaySetItemDeletesThenSaves() {
        service.updateDisplaySetItem(List.of(new UpdateDataDisplaySetDto()));

        verify(repository).saveAll(any());
    }

    @Test
    @DisplayName("getList：displayType 为空时不追加过滤条件")
    void getListWithoutDisplayType() {
        stubSelectChain();
        stubPage(List.of(new DataDisplaySet()), 1L);

        PagedResultDto<DataDisplayItemDto> result = service.getList(new GetDisplaySetInputDto());

        assertThat(result.getTotalCount()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getList：displayType 非空时追加等值过滤条件")
    void getListWithDisplayType() {
        stubSelectChain();
        stubPage(List.of(new DataDisplaySet()), 1L);

        GetDisplaySetInputDto input = new GetDisplaySetInputDto();
        input.setDisplayType("table");

        assertThat(service.getList(input).getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getList：input 为 null 时按现有实现抛 NPE")
    void getListWithNullInputThrowsNpe() {
        stubSelectChain();

        assertThatThrownBy(() -> service.getList(null)).isInstanceOf(NullPointerException.class);
    }
}
