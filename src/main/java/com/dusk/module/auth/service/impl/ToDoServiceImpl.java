package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dusk.common.rpc.auth.dto.ToDoDto;
import com.dusk.common.rpc.auth.enums.ToDoTargetType;
import com.dusk.module.auth.entity.Todo;
import com.dusk.module.auth.entity.TodoRead;
import com.dusk.module.auth.entity.User;
import com.github.dozermapper.core.Mapper;
import com.querydsl.core.types.QBean;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.common.core.entity.CreationEntity;
import com.dusk.common.core.jpa.querydsl.QBeanBuilder;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.module.auth.dto.todo.GetTodosInput;
import com.dusk.module.auth.dto.todo.TodoInfoDto;
import com.dusk.module.auth.entity.*;
import com.dusk.module.auth.enums.ToDoMQTTTypeEnum;
import com.dusk.module.auth.manage.IUserManage;
import com.dusk.module.auth.repository.IToDoRepository;
import com.dusk.module.auth.service.IToDoService;
import com.dusk.module.auth.service.ITodoIgnoreService;
import com.dusk.module.auth.service.ITodoReadService;
import com.dusk.module.auth.service.ToDoPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-08-04 14:39
 */
@Service
@Transactional
@Slf4j
public class ToDoServiceImpl extends BaseService<Todo, IToDoRepository> implements IToDoService {
    private static final int TITLE_MAX_LENGTH = 200;
    @Autowired
    Mapper dozerMapper;
    @Autowired
    JPAQueryFactory queryFactory;
    @Autowired
    SecurityUtils securityUtils;
    @Autowired
    IUserManage userManage;
    @Autowired
    ToDoPushService toDoPushService;
    @Autowired
    private ITodoIgnoreService todoIgnoreService;
    @Autowired
    private ITodoReadService todoReadService;


    @Override
    public void addTodo(@Valid ToDoDto input) {
        //校验数据有效性
        Todo data = dozerMapper.map(input, Todo.class);
        if(input.getTitle().length() > TITLE_MAX_LENGTH){
            data.setTitle(StrUtil.sub(input.getTitle(), 0, TITLE_MAX_LENGTH) + "...");
        }

        User currentUser = userManage.getCurrentUser();
        if(StrUtil.isEmpty(input.getStarter()) && currentUser != null){//设置默认发起人信息
            data.setStarter(currentUser.getName());
        }
        if(currentUser != null){//设置上一提交人信息
            data.setPreHandler(currentUser.getName());
        }

        for (String temp : input.getTargetData()) {
            data.addPermission(temp);
        }
        if (input.isFilterStation()) {
            data.setOrgId(DataFilterContextHolder.getDefaultOrgId());
        }
        Long id = save(data).getId();
        log.info("成功添加一条待办 类型：{},数据id：{}", input.getType(), input.getBusinessId());
        toDoPushService.pushMsg(data, input, ToDoMQTTTypeEnum.ADD);
    }

    @Override
    public void finishTodo(String type, String businessId) {
        QTodo todo = QTodo.todo;
        List<Todo> todos = repository.findByTypeAndBusinessIdAndFinish(type, businessId, false);
        if (todos.isEmpty()) {
            return;
        }

        long execute = queryFactory.update(todo).set(todo.finish, true)
                .set(todo.finishUseId, securityUtils.getCurrentUser() == null ? null : securityUtils.getCurrentUser().getId())
                .set(todo.finishTime, LocalDateTime.now())
                .where(todo.type.eq(type).and(todo.businessId.eq(businessId).and(todo.finish.eq(false)))).execute();
        log.info("成功完成 待办类型：" + type + " 业务id：" + businessId + "的待办" + execute + "条");

        todos.forEach(p -> {
            toDoPushService.pushMqttMsg(p, ToDoMQTTTypeEnum.FINISH);
        });
    }

    @Override
    public void ignoreTodo(Long todoId) {
        findById(todoId).ifPresent(todo -> {
            boolean flag = todoIgnoreService.ignoreTodo(todoId);
            if(flag){
                toDoPushService.pushIgnoreMsg(todo, LoginUserIdContextHolder.getUserId());
            }
        });
    }

    @Override
    public Page<TodoInfoDto> getTodos(GetTodosInput input) {

        if (StringUtils.isBlank(input.getSorting())) {
            input.setSorting(CreationEntity.Fields.createTime);
        }

        BooleanExpression expression = QTodo.todo.finish.eq(false);

        //过滤场站
        String dataFilterId = DataFilterContextHolder.getDataFilterId();
        BooleanExpression orgFilter = QTodo.todo.orgId.isNull();
        if (StringUtils.isNotEmpty(dataFilterId)) {
            List<Long> orgIds = Arrays.stream(DataFilterContextHolder.getDataFilterId().split(",")).map(Long::new).collect(Collectors.toList());
            orgFilter = orgFilter.or(QTodo.todo.orgId.in(orgIds));
        }
        expression = expression.and(orgFilter);


        if (StringUtils.isNotBlank(input.getTypeName())) {
            expression = expression.and(QTodo.todo.typeName.contains(input.getTypeName()));
        }
        if (StringUtils.isNotBlank(input.getTitle())) {
            expression = expression.and(QTodo.todo.title.contains(input.getTitle()));
        }
        if (input.getType() != null && input.getType().size() > 0) {
            expression = expression.and(QTodo.todo.type.in(input.getType()).or(QTodo.todo.subType.in(input.getType())));
        }
        QTodoPermission todoPermission = QTodoPermission.todoPermission;
        User currentUser = userManage.getCurrentUser();
        BooleanExpression permissionExpression = QTodo.todo.targetType.eq(ToDoTargetType.UserId).and(todoPermission.permission.eq(currentUser.getId().toString()));
        List<String> roles = currentUser.getUserRoles().stream().map(Role::getRoleName).collect(Collectors.toList());
        if (!roles.isEmpty()) {
            permissionExpression = permissionExpression.or(QTodo.todo.targetType.eq(ToDoTargetType.Role).and(todoPermission.permission.in(roles)));
        }

        BooleanExpression temp = QTodo.todo.targetType.eq(ToDoTargetType.Permission);
        if (!currentUser.isAdmin()) {
            List<String> currentUserPermissions = userManage.getCurrentUserPermissions();
            if (currentUserPermissions.size() > 0) {
                temp = temp.and(todoPermission.permission.in(currentUserPermissions));
                permissionExpression = permissionExpression.or(temp);
            }
        } else {
            permissionExpression = permissionExpression.or(temp);
        }

        //not ignore
        QTodoIgnore qTodoIgnore = QTodoIgnore.todoIgnore;
        BooleanExpression notIgnoreExpression =
                JPAExpressions.selectFrom(qTodoIgnore).where(qTodoIgnore.todoId.eq(QTodo.todo.id).and(qTodoIgnore.createId.eq(currentUser.getId()))).notExists();

        expression = expression.and(permissionExpression).and(notIgnoreExpression);

        QTodo qTodo = QTodo.todo;
        QTodoPermission qTodoPermission = QTodoPermission.todoPermission;
        QBean<TodoInfoDto> qBean = QBeanBuilder.create(TodoInfoDto.class).appendQEntity(qTodo).build();
        JPAQuery<TodoInfoDto> query = queryFactory.select(qBean).from(qTodo)
                                                  .leftJoin(qTodoPermission).on(QTodo.todo.id.eq(QTodoPermission.todoPermission.todoId))
                                                  .where(expression).distinct();
        Page<TodoInfoDto> page = (Page<TodoInfoDto>) page(query, input.getPageable());
        List<TodoInfoDto> todoList = page.getContent();
        List<TodoRead> todoReadList = todoReadService.findByUserId(currentUser.getId());
        todoList.forEach(todo->{
            boolean anyMatch = todoReadList.stream().anyMatch(read -> read.getTodoId().equals(todo.getId()));
            todo.setHasRead(anyMatch);
        });
        return page;
    }

    @Override
    public void read(Long id) {
        todoReadService.read(id);
    }


}
