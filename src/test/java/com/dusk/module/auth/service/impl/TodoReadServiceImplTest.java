package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.module.auth.entity.TodoRead;
import com.dusk.module.auth.repository.ITodoReadRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TodoReadServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class TodoReadServiceImplTest {

    private static final long USER_ID = 7L;

    @Mock
    private ITodoReadRepository repository;

    private TodoReadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TodoReadServiceImpl();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "todoReadRepository", repository);
        LoginUserIdContextHolder.setUserId(USER_ID);
    }

    @AfterEach
    void tearDown() {
        LoginUserIdContextHolder.clear();
    }

    @Test
    @DisplayName("read：无已读记录时新增一条")
    void readCreatesRecordWhenAbsent() {
        when(repository.findByTodoIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        service.read(1L);

        ArgumentCaptor<TodoRead> captor = ArgumentCaptor.forClass(TodoRead.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTodoId()).isEqualTo(1L);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("read：已存在已读记录时不重复新增")
    void readSkipsWhenRecordExists() {
        when(repository.findByTodoIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(new TodoRead()));

        service.read(1L);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("findByUserId：直接返回仓储查询结果")
    void findByUserIdDelegates() {
        TodoRead read = new TodoRead();
        when(repository.findByUserId(USER_ID)).thenReturn(List.of(read));

        assertThat(service.findByUserId(USER_ID)).containsExactly(read);
    }
}
