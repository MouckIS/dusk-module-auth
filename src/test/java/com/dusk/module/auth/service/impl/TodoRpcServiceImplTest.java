package com.dusk.module.auth.service.impl;

import com.dusk.common.rpc.auth.dto.ToDoDto;
import com.dusk.common.rpc.auth.enums.ToDoTargetType;
import com.dusk.module.auth.entity.Todo;
import com.dusk.module.auth.service.IToDoService;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TodoRpcServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TodoRpcServiceImplTest {

    @Mock
    private IToDoService toDoService;
    @Mock
    private JPAQueryFactory jpaQueryFactory;

    private TodoRpcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TodoRpcServiceImpl();
        ReflectionTestUtils.setField(service, "toDoService", toDoService);
        ReflectionTestUtils.setField(service, "jpaQueryFactory", jpaQueryFactory);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubOldTodos(List<Todo> oldTodos) {
        JPAQuery query = mock(JPAQuery.class);
        when(jpaQueryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.fetch()).thenReturn(oldTodos);
    }

    private static Todo todo(String businessId) {
        Todo todo = new Todo();
        todo.setBusinessId(businessId);
        todo.setType("t");
        todo.setTypeName("tn");
        todo.setTodoPermissions(new ArrayList<>());
        return todo;
    }

    private static ToDoDto dto(String businessId) {
        ToDoDto dto = new ToDoDto();
        dto.setBusinessId(businessId);
        dto.setType("t");
        dto.setTypeName("tn");
        dto.setTitle("title");
        dto.setState("s");
        dto.setTargetType(ToDoTargetType.UserId);
        dto.setTargetData(new String[]{"1"});
        dto.setExtensions("{}");
        return dto;
    }

    @Test
    @DisplayName("addTodoAndFinishOld：先结束旧待办再新增")
    void addTodoAndFinishOldFinishesThenAdds() {
        ToDoDto input = dto("b1");

        service.addTodoAndFinishOld(input);

        verify(toDoService).finishTodo("t", "b1");
        verify(toDoService).addTodo(input);
    }

    @Test
    @DisplayName("syncActivitiTask：入参为 null 时直接返回")
    void syncActivitiTaskReturnsEarlyWhenNull() {
        service.syncActivitiTask("p", null);

        verify(toDoService, never()).addTodo(any());
    }

    @Test
    @DisplayName("syncActivitiTask：旧待办为空且入参为空时不做任何处理")
    void syncActivitiTaskWithNoOldTodosAndEmptyInput() {
        stubOldTodos(List.of());

        service.syncActivitiTask("p", new ArrayList<>());

        verify(toDoService, never()).addTodo(any());
    }

    @Test
    @DisplayName("syncActivitiTask：已存在的待办从入参中移除，其余新增")
    void syncActivitiTaskRemovesExistingAndAddsRest() {
        stubOldTodos(List.of(todo("p1")));
        ToDoDto matched = dto("p1");
        ToDoDto extra = dto("p2");
        List<ToDoDto> input = new ArrayList<>(List.of(matched, extra));

        service.syncActivitiTask("p", input);

        assertThat(input).containsExactly(extra);
        verify(toDoService, never()).finishTodo("t", "p1");
        verify(toDoService).addTodo(extra);
    }

    @Test
    @DisplayName("syncActivitiTask：旧待办不在入参中时结束该待办")
    void syncActivitiTaskFinishesMissingOldTodo() {
        stubOldTodos(List.of(todo("p9")));
        ToDoDto input = dto("p1");
        List<ToDoDto> list = new ArrayList<>(List.of(input));

        service.syncActivitiTask("p", list);

        verify(toDoService).finishTodo("t", "p9");
        verify(toDoService).addTodo(input);
    }

    @Test
    @DisplayName("syncActivitiTaskAssigneeChanged：入参为 null 时直接返回")
    void syncActivitiTaskAssigneeChangedReturnsEarlyWhenNull() {
        service.syncActivitiTaskAssigneeChanged("p", null);

        verify(toDoService, never()).addTodo(any());
    }

    @Test
    @DisplayName("syncActivitiTaskAssigneeChanged：旧待办为空且入参为空时不做任何处理")
    void syncActivitiTaskAssigneeChangedWithNoOldTodosAndEmptyInput() {
        stubOldTodos(List.of());

        service.syncActivitiTaskAssigneeChanged("p", new ArrayList<>());

        verify(toDoService, never()).addTodo(any());
    }

    @Test
    @DisplayName("syncActivitiTaskAssigneeChanged：命中的旧待办被结束，入参全部新增")
    void syncActivitiTaskAssigneeChangedFinishesMatched() {
        stubOldTodos(List.of(todo("p1")));
        ToDoDto input = dto("p1");
        List<ToDoDto> list = new ArrayList<>(List.of(input));

        service.syncActivitiTaskAssigneeChanged("p", list);

        verify(toDoService).finishTodo("t", "p1");
        verify(toDoService).addTodo(input);
    }

    @Test
    @DisplayName("syncActivitiTaskAssigneeChanged：未命中的旧待办不处理")
    void syncActivitiTaskAssigneeChangedIgnoresUnmatched() {
        stubOldTodos(List.of(todo("p9")));
        ToDoDto input = dto("p1");
        List<ToDoDto> list = new ArrayList<>(List.of(input));

        service.syncActivitiTaskAssigneeChanged("p", list);

        verify(toDoService, never()).finishTodo(any(), any());
        verify(toDoService).addTodo(input);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubFindByPermission(List<Todo> todos) {
        JPAQuery subQuery = mock(JPAQuery.class);
        JPAQuery mainQuery = mock(JPAQuery.class);
        when(jpaQueryFactory.selectOne()).thenReturn(subQuery);
        when(jpaQueryFactory.selectFrom(any(EntityPath.class))).thenReturn(mainQuery);
        when(subQuery.from(any(EntityPath.class))).thenReturn(subQuery);
        when(subQuery.where(any(Predicate.class))).thenReturn(subQuery);
        when(subQuery.exists()).thenReturn(mock(BooleanExpression.class));
        when(mainQuery.where(any(Predicate.class))).thenReturn(mainQuery);
        when(mainQuery.fetch()).thenReturn(todos);
    }

    @Test
    @DisplayName("findByPermission：typeName 为空时不追加类型过滤条件")
    void findByPermissionWithoutTypeName() {
        stubFindByPermission(List.of(todo("p1")));

        List<ToDoDto> result = service.findByPermission(ToDoTargetType.UserId, "t", "  ", List.of("perm"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBusinessId()).isEqualTo("p1");
    }

    @Test
    @DisplayName("findByPermission：typeName 非空时追加类型过滤条件")
    void findByPermissionWithTypeName() {
        stubFindByPermission(List.of(todo("p1")));

        List<ToDoDto> result = service.findByPermission(ToDoTargetType.UserId, "t", "tn", List.of("perm"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTypeName()).isEqualTo("tn");
    }
}
