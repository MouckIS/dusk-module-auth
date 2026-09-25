package com.dusk.module.auth.service.impl;

import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.dto.loginlog.ListUserLoginLogInput;
import com.dusk.module.auth.dto.loginlog.UserLoginLogDto;
import com.dusk.module.auth.entity.UserLoginLog;
import com.dusk.module.auth.enums.LoginLogType;
import com.dusk.module.auth.listener.LogInOutEvent;
import com.dusk.module.auth.repository.IUserLoginLogRepository;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserLoginLogServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class UserLoginLogServiceImplTest {

    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private IUserLoginLogRepository repository;

    private UserLoginLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new UserLoginLogServiceImpl());
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "repository", repository);
    }

    @SuppressWarnings("rawtypes")
    private void stubQueryChain() {
        JPAQuery query = mock(JPAQuery.class);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.from(any(EntityPath.class))).thenReturn(query);
        when(query.leftJoin(any(EntityPath.class))).thenReturn(query);
        when(query.on(any(Predicate.class))).thenReturn(query);
    }

    @SuppressWarnings("unchecked")
    private void stubPage(List<UserLoginLogDto> content, long totalElements) {
        Page<UserLoginLogDto> page = mock(Page.class);
        when(page.getContent()).thenReturn(content);
        when(page.getTotalElements()).thenReturn(totalElements);
        doReturn(page).when(service).page(any(JPAQuery.class), any(Pageable.class));
    }

    @Test
    @DisplayName("saveLog：拷贝事件并落库")
    void saveLogPersistsConvertedEntity() {
        LogInOutEvent event = new LogInOutEvent(LoginLogType.LOGIN_IN, LocalDateTime.now(), true);
        event.setUserName("admin");

        service.saveLog(event);

        verify(repository).save(any(UserLoginLog.class));
    }

    @Test
    @DisplayName("listLog：无任何过滤条件时正常返回分页数据")
    void listLogWithoutFilters() {
        stubQueryChain();
        UserLoginLogDto dto = new UserLoginLogDto();
        dto.setLogType(LoginLogType.LOGIN_IN);
        stubPage(List.of(dto), 1L);

        PagedResultDto<UserLoginLogDto> result = service.listLog(new ListUserLoginLogInput());

        assertThat(result.getTotalCount()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getLogTypeName()).isNotNull();
    }

    @Test
    @DisplayName("listLog：所有过滤条件均生效")
    void listLogWithAllFilters() {
        stubQueryChain();
        UserLoginLogDto dto = new UserLoginLogDto();
        dto.setLogType(LoginLogType.LOGIN_OUT);
        stubPage(List.of(dto), 1L);

        ListUserLoginLogInput input = new ListUserLoginLogInput();
        input.setUserName("admin");
        input.setLogType(LoginLogType.LOGIN_OUT);
        input.setBeginTime(LocalDateTime.now().minusDays(1));
        input.setEndTime(LocalDateTime.now());

        PagedResultDto<UserLoginLogDto> result = service.listLog(input);

        assertThat(result.getItems()).hasSize(1);
    }
}
