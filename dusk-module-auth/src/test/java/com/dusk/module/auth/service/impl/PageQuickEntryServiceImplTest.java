package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.dto.quickentry.GetQuickSetListDto;
import com.dusk.module.auth.dto.quickentry.QuickEntryListDto;
import com.dusk.module.auth.dto.quickentry.UpdatePageQuickSetDto;
import com.dusk.module.auth.entity.quickentry.PageQuickEntry;
import com.dusk.module.auth.repository.pagequickentry.IPageQuickEntryRepository;
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
 * {@link PageQuickEntryServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class PageQuickEntryServiceImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JPAQueryFactory queryFactory;
    @Mock
    private IPageQuickEntryRepository repository;

    private PageQuickEntryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new PageQuickEntryServiceImpl());
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
    private void stubPage(List<PageQuickEntry> content, long total) {
        Page<PageQuickEntry> page = mock(Page.class);
        when(page.getContent()).thenReturn(content);
        when(page.getTotalElements()).thenReturn(total);
        doReturn(page).when(service).page(any(JPAQuery.class), any(Pageable.class));
    }

    @Test
    @DisplayName("updateQuickSet：删除旧设置后批量新增")
    void updateQuickSetDeletesThenSaves() {
        List<UpdatePageQuickSetDto> input = List.of(new UpdatePageQuickSetDto());

        service.updateQuickSet(input);

        verify(repository).saveAll(any());
    }

    @Test
    @DisplayName("getQuickSetList：routeName 为空时不追加过滤条件")
    void getQuickSetListWithoutRouteNameFilter() {
        stubSelectChain();
        stubPage(List.of(new PageQuickEntry()), 1L);

        PagedResultDto<QuickEntryListDto> result = service.getQuickSetList(new GetQuickSetListDto());

        assertThat(result.getTotalCount()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getQuickSetList：routeName 非空时追加模糊过滤条件")
    void getQuickSetListWithRouteNameFilter() {
        stubSelectChain();
        stubPage(List.of(new PageQuickEntry()), 1L);

        GetQuickSetListDto input = new GetQuickSetListDto();
        input.setRouteName("home");

        assertThat(service.getQuickSetList(input).getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getQuickSetList：input 为 null 时按现有实现抛 NPE")
    void getQuickSetListWithNullInputThrowsNpe() {
        stubSelectChain();

        assertThatThrownBy(() -> service.getQuickSetList(null)).isInstanceOf(NullPointerException.class);
    }
}
