package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.module.auth.entity.TodoIgnore;
import com.dusk.module.auth.repository.ITodoIgnoreRepository;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TodoIgnoreServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class TodoIgnoreServiceImplTest {

    private static final long USER_ID = 7L;

    @Mock
    private ITodoIgnoreRepository repository;

    private TodoIgnoreServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TodoIgnoreServiceImpl();
        ReflectionTestUtils.setField(service, "repository", repository);
        LoginUserIdContextHolder.setUserId(USER_ID);
    }

    @AfterEach
    void tearDown() {
        LoginUserIdContextHolder.clear();
    }

    @Test
    @DisplayName("ignoreTodo：尚无忽略记录时新增并返回 true")
    void ignoreTodoCreatesRecordWhenAbsent() {
        when(repository.exists(any(Predicate.class))).thenReturn(false);

        assertThat(service.ignoreTodo(1L)).isTrue();

        verify(repository).save(any(TodoIgnore.class));
    }

    @Test
    @DisplayName("ignoreTodo：已存在忽略记录时直接返回 false")
    void ignoreTodoReturnsFalseWhenPresent() {
        when(repository.exists(any(Predicate.class))).thenReturn(true);

        assertThat(service.ignoreTodo(1L)).isFalse();

        verify(repository, never()).save(any());
    }
}
