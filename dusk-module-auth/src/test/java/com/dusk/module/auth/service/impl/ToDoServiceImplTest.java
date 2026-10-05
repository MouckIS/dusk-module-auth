package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.module.auth.dto.ToDoDto;
import com.dusk.module.auth.enums.ToDoTargetType;
import com.dusk.module.auth.dto.todo.GetTodosInput;
import com.dusk.module.auth.dto.todo.TodoInfoDto;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.Todo;
import com.dusk.module.auth.entity.TodoRead;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.enums.ToDoMQTTTypeEnum;
import com.dusk.module.auth.manage.IUserManage;
import com.dusk.module.auth.repository.IToDoRepository;
import com.dusk.module.auth.service.ITodoIgnoreService;
import com.dusk.module.auth.service.ITodoReadService;
import com.dusk.module.auth.service.ToDoPushService;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ToDoServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ToDoServiceImplTest {

    private static final long CURRENT_USER_ID = 3L;

    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private IUserManage userManage;
    @Mock
    private ToDoPushService toDoPushService;
    @Mock
    private ITodoIgnoreService todoIgnoreService;
    @Mock
    private ITodoReadService todoReadService;
    @Mock
    private IToDoRepository repository;

    private ToDoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new ToDoServiceImpl());
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "securityUtils", securityUtils);
        ReflectionTestUtils.setField(service, "userManage", userManage);
        ReflectionTestUtils.setField(service, "toDoPushService", toDoPushService);
        ReflectionTestUtils.setField(service, "todoIgnoreService", todoIgnoreService);
        ReflectionTestUtils.setField(service, "todoReadService", todoReadService);
        ReflectionTestUtils.setField(service, "repository", repository);
    }

    @AfterEach
    void tearDown() {
        DataFilterContextHolder.clear();
    }

    private static ToDoDto toDoDto(String title) {
        ToDoDto dto = new ToDoDto();
        dto.setType("OA");
        dto.setTypeName("办公");
        dto.setTitle(title);
        dto.setState("PENDING");
        dto.setTargetType(ToDoTargetType.UserId);
        dto.setTargetData(new String[]{"3"});
        dto.setBusinessId("BIZ-1");
        return dto;
    }

    private static User currentUser(boolean admin, List<Role> roles) {
        User user = new User();
        user.setId(CURRENT_USER_ID);
        user.setName("tester");
        user.setAdmin(admin);
        user.setUserRoles(new ArrayList<>(roles));
        return user;
    }

    /** SecurityUtils 返回的是登录上下文 UserContext，与 IUserManage 返回的 User 不是同一类型。 */
    private static UserContext loginUserContext() {
        UserContext context = new UserContext();
        context.setId(CURRENT_USER_ID);
        return context;
    }

    private void stubSaveAssignsId() {
        when(repository.save(any(Todo.class))).thenAnswer(invocation -> {
            Todo todo = invocation.getArgument(0);
            if (todo.getId() == null) {
                todo.setId(500L);
            }
            return todo;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPAUpdateClause stubUpdate(long affected) {
        JPAUpdateClause update = mock(JPAUpdateClause.class, Answers.RETURNS_SELF);
        when(queryFactory.update(any(com.querydsl.core.types.EntityPath.class))).thenReturn(update);
        when(update.execute()).thenReturn(affected);
        return update;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JPAQuery<?> stubSelect() {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        return query;
    }

    @SuppressWarnings("unchecked")
    private void stubPage(List<TodoInfoDto> content) {
        Page<TodoInfoDto> page = new PageImpl<>(content, PageRequest.of(0, 10), content.size());
        doReturn(page).when(service).page(any(JPAQuery.class), any(Pageable.class));
    }

    // ---------------- addTodo ----------------

    @Test
    @DisplayName("addTodo：标题超长时截断为 200 字符并追加省略号")
    void addTodoTruncatesLongTitle() {
        stubSaveAssignsId();
        when(userManage.getCurrentUser()).thenReturn(null);

        ToDoDto input = toDoDto("A".repeat(300));
        service.addTodo(input);

        verify(repository).save(any(Todo.class));
        verify(toDoPushService).pushMsg(any(Todo.class), any(ToDoDto.class), any(ToDoMQTTTypeEnum.class));
    }

    @Test
    @DisplayName("addTodo：标题在长度边界内时不截断")
    void addTodoKeepsTitleWithinBoundary() {
        stubSaveAssignsId();
        when(userManage.getCurrentUser()).thenReturn(null);

        service.addTodo(toDoDto("B".repeat(200)));

        verify(repository).save(any(Todo.class));
    }

    @Test
    @DisplayName("addTodo：存在当前用户时回填发起人与上一提交人")
    void addTodoFillsStarterAndPreHandler() {
        stubSaveAssignsId();
        when(userManage.getCurrentUser()).thenReturn(currentUser(false, List.of()));

        service.addTodo(toDoDto("普通标题"));

        verify(toDoPushService).pushMsg(any(Todo.class), any(ToDoDto.class), any(ToDoMQTTTypeEnum.class));
    }

    @Test
    @DisplayName("addTodo：已显式指定发起人时不覆盖")
    void addTodoKeepsExplicitStarter() {
        stubSaveAssignsId();
        when(userManage.getCurrentUser()).thenReturn(currentUser(false, List.of()));

        ToDoDto input = toDoDto("普通标题");
        input.setStarter("指定发起人");
        service.addTodo(input);

        verify(repository).save(any(Todo.class));
    }

    @Test
    @DisplayName("addTodo：filterStation=true 时写入默认场站 id")
    void addTodoAppliesFilterStation() {
        stubSaveAssignsId();
        when(userManage.getCurrentUser()).thenReturn(currentUser(false, List.of()));
        DataFilterContextHolder.setDataFilterId("88,99");

        ToDoDto input = toDoDto("场站待办");
        input.setFilterStation(true);
        service.addTodo(input);

        verify(repository).save(any(Todo.class));
    }

    @Test
    @DisplayName("addTodo：filterStation=true 且无默认场站时 orgId 为 null")
    void addTodoWithFilterStationButNoOrg() {
        stubSaveAssignsId();
        when(userManage.getCurrentUser()).thenReturn(null);

        ToDoDto input = toDoDto("无场站待办");
        input.setFilterStation(true);
        service.addTodo(input);

        verify(repository).save(any(Todo.class));
    }

    // ---------------- finishTodo ----------------

    @Test
    @DisplayName("finishTodo：无未完成待办时直接返回")
    void finishTodoReturnsWhenNothingPending() {
        when(repository.findByTypeAndBusinessIdAndFinish("OA", "BIZ-1", false)).thenReturn(List.of());

        service.finishTodo("OA", "BIZ-1");

        verify(queryFactory, never()).update(any(com.querydsl.core.types.EntityPath.class));
        verify(toDoPushService, never()).pushMqttMsg(any(Todo.class), any(ToDoMQTTTypeEnum.class));
    }

    @Test
    @DisplayName("finishTodo：存在未完成待办时批量置完成后逐个推送")
    void finishTodoUpdatesAndPushes() {
        Todo todo = new Todo();
        todo.setId(1L);
        when(repository.findByTypeAndBusinessIdAndFinish("OA", "BIZ-1", false)).thenReturn(List.of(todo));
        stubUpdate(1L);
        when(securityUtils.getCurrentUser()).thenReturn(loginUserContext());

        service.finishTodo("OA", "BIZ-1");

        verify(toDoPushService).pushMqttMsg(todo, ToDoMQTTTypeEnum.FINISH);
    }

    @Test
    @DisplayName("finishTodo：无当前登录用户时完成人 id 写 null")
    void finishTodoWithoutLoginUser() {
        Todo todo = new Todo();
        when(repository.findByTypeAndBusinessIdAndFinish("OA", "BIZ-1", false)).thenReturn(List.of(todo));
        stubUpdate(1L);
        when(securityUtils.getCurrentUser()).thenReturn(null);

        service.finishTodo("OA", "BIZ-1");

        verify(toDoPushService).pushMqttMsg(todo, ToDoMQTTTypeEnum.FINISH);
    }

    // ---------------- ignoreTodo / read ----------------

    @Test
    @DisplayName("ignoreTodo：待办不存在时不调用忽略服务")
    void ignoreTodoSkipsWhenTodoAbsent() {
        when(repository.findById(1L)).thenReturn(java.util.Optional.empty());

        service.ignoreTodo(1L);

        verify(todoIgnoreService, never()).ignoreTodo(any());
    }

    @Test
    @DisplayName("ignoreTodo：忽略成功时推送忽略消息")
    void ignoreTodoPushesWhenIgnored() {
        Todo todo = new Todo();
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(todo));
        when(todoIgnoreService.ignoreTodo(1L)).thenReturn(true);
        LoginUserIdContextHolder.setUserId(CURRENT_USER_ID);

        service.ignoreTodo(1L);

        verify(toDoPushService).pushIgnoreMsg(todo, CURRENT_USER_ID);
        LoginUserIdContextHolder.clear();
    }

    @Test
    @DisplayName("ignoreTodo：忽略失败时不推送")
    void ignoreTodoSkipsPushWhenNotIgnored() {
        Todo todo = new Todo();
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(todo));
        when(todoIgnoreService.ignoreTodo(1L)).thenReturn(false);

        service.ignoreTodo(1L);

        verify(toDoPushService, never()).pushIgnoreMsg(any(Todo.class), any());
    }

    @Test
    @DisplayName("read：委派给已读服务")
    void readDelegates() {
        service.read(9L);

        verify(todoReadService).read(9L);
    }

    // ---------------- getTodos ----------------

    @Test
    @DisplayName("getTodos：管理员且无数据过滤时返回全部待办并标记已读状态")
    void getTodosForAdmin() {
        stubSelect();
        User admin = currentUser(true, List.of());
        when(userManage.getCurrentUser()).thenReturn(admin);
        TodoRead read = new TodoRead();
        read.setTodoId(1L);
        when(todoReadService.findByUserId(CURRENT_USER_ID)).thenReturn(List.of(read));

        TodoInfoDto dto = new TodoInfoDto();
        dto.setId(1L);
        stubPage(List.of(dto));

        List<TodoInfoDto> content = service.getTodos(new GetTodosInput()).getContent();

        assertThat(content).hasSize(1);
        assertThat(content.getFirst().getHasRead()).isTrue();
    }

    @Test
    @DisplayName("getTodos：非管理员且有权限时按权限过滤")
    void getTodosForNonAdminWithPermissions() {
        stubSelect();
        Role role = new Role();
        role.setRoleName("operator");
        User user = currentUser(false, List.of(role));
        when(userManage.getCurrentUser()).thenReturn(user);
        when(userManage.getCurrentUserPermissions()).thenReturn(List.of("Pages.Todo"));
        when(todoReadService.findByUserId(CURRENT_USER_ID)).thenReturn(List.of());

        TodoInfoDto dto = new TodoInfoDto();
        dto.setId(2L);
        stubPage(List.of(dto));

        assertThat(service.getTodos(new GetTodosInput()).getContent().getFirst().getHasRead()).isFalse();
    }

    @Test
    @DisplayName("getTodos：非管理员且无权限时仅按用户/角色过滤")
    void getTodosForNonAdminWithoutPermissions() {
        stubSelect();
        User user = currentUser(false, List.of());
        when(userManage.getCurrentUser()).thenReturn(user);
        when(userManage.getCurrentUserPermissions()).thenReturn(List.of());
        when(todoReadService.findByUserId(CURRENT_USER_ID)).thenReturn(List.of());
        stubPage(List.of());

        assertThat(service.getTodos(new GetTodosInput()).getContent()).isEmpty();
    }

    @Test
    @DisplayName("getTodos：全量过滤条件命中时逐条拼接")
    void getTodosWithAllFilters() {
        stubSelect();
        User admin = currentUser(true, List.of());
        when(userManage.getCurrentUser()).thenReturn(admin);
        when(todoReadService.findByUserId(CURRENT_USER_ID)).thenReturn(List.of());
        DataFilterContextHolder.setDataFilterId("11,22");
        stubPage(List.of());

        GetTodosInput input = new GetTodosInput();
        input.setSorting("title");
        input.setTypeName("办公");
        input.setTitle("标题");
        input.setType(List.of("OA", "HR"));

        assertThat(service.getTodos(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("getTodos：type 为空集合时跳过类型条件")
    void getTodosWithEmptyTypeList() {
        stubSelect();
        User admin = currentUser(true, List.of());
        when(userManage.getCurrentUser()).thenReturn(admin);
        when(todoReadService.findByUserId(CURRENT_USER_ID)).thenReturn(List.of());
        stubPage(List.of());

        GetTodosInput input = new GetTodosInput();
        input.setType(List.of());

        assertThat(service.getTodos(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("getTodos：未指定排序时使用创建时间作为默认排序")
    void getTodosDefaultsToCreateTimeSort() {
        stubSelect();
        when(userManage.getCurrentUser()).thenReturn(currentUser(true, List.of()));
        when(todoReadService.findByUserId(CURRENT_USER_ID)).thenReturn(List.of());
        stubPage(List.of());

        GetTodosInput input = new GetTodosInput();
        input.setSorting("   ");

        assertThat(service.getTodos(input).getContent()).isEmpty();
        assertThat(input.getSorting()).isNotBlank();
    }

    @Test
    @DisplayName("getTodos：角色列表非空时追加角色维度权限条件")
    void getTodosWithRoles() {
        stubSelect();
        Role role = new Role();
        role.setRoleName("manager");
        when(userManage.getCurrentUser()).thenReturn(currentUser(false, List.of(role)));
        when(userManage.getCurrentUserPermissions()).thenReturn(List.of());
        when(todoReadService.findByUserId(CURRENT_USER_ID)).thenReturn(List.of());
        stubPage(List.of());

        assertThat(service.getTodos(new GetTodosInput()).getContent()).isEmpty();
    }

    @Test
    @DisplayName("addTodo：默认构造的 ToDoDto 不设置场站过滤")
    void addTodoWithoutFilterStation() {
        stubSaveAssignsId();
        when(userManage.getCurrentUser()).thenReturn(currentUser(false, List.of()));

        ToDoDto input = toDoDto("标题");
        input.setFilterStation(false);
        service.addTodo(input);

        verify(repository).save(any(Todo.class));
        verify(toDoPushService).pushMsg(any(Todo.class), any(ToDoDto.class), any(ToDoMQTTTypeEnum.class));
    }

    @Test
    @DisplayName("finishTodo：多个待办时逐个推送完成消息")
    void finishTodoPushesForEachTodo() {
        Todo first = new Todo();
        Todo second = new Todo();
        when(repository.findByTypeAndBusinessIdAndFinish(anyString(), anyString(), anyBoolean()))
                .thenReturn(List.of(first, second));
        stubUpdate(2L);
        when(securityUtils.getCurrentUser()).thenReturn(null);

        service.finishTodo("OA", "BIZ-1");

        verify(toDoPushService).pushMqttMsg(first, ToDoMQTTTypeEnum.FINISH);
        verify(toDoPushService).pushMqttMsg(second, ToDoMQTTTypeEnum.FINISH);
    }
}
