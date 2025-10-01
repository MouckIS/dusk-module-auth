package com.dusk.module.auth.service.impl;

import com.dusk.common.rpc.auth.dto.ToDoDto;
import com.dusk.common.rpc.auth.enums.ToDoTargetType;
import com.dusk.common.rpc.auth.service.ITodoRpcService;
import com.dusk.module.auth.entity.TodoPermission;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.module.auth.entity.QTodo;
import com.dusk.module.auth.entity.QTodoPermission;
import com.dusk.module.auth.entity.Todo;
import com.dusk.module.auth.service.IToDoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-08-04 16:45
 */
@Service(retries = 0, timeout = 2000)
@Transactional
@Slf4j
public class TodoRpcServiceImpl implements ITodoRpcService {
    @Autowired
    IToDoService toDoService;

    @Autowired
    JPAQueryFactory jpaQueryFactory;

    @Override
    public void addTodo(ToDoDto input) {
        toDoService.addTodo(input);
    }

    @Override
    public void finishTodo(String type, String businessId) {
        toDoService.finishTodo(type, businessId);
    }

    @Override
    public void addTodoAndFinishOld(ToDoDto input) {
        toDoService.finishTodo(input.getType(), input.getBusinessId());
        toDoService.addTodo(input);
    }

    @Override
    public void syncActivitiTask(String processInstanceId, List<ToDoDto> input) {
        if (input != null) {
            QTodo qTodo = QTodo.todo;
            //完成旧的待办
            List<Todo> oldTodos = jpaQueryFactory.selectFrom(qTodo).where(qTodo.businessId.startsWith(processInstanceId)).fetch();
            oldTodos.forEach(p -> {
                Optional<ToDoDto> compare = input.stream().filter(o -> o.getBusinessId().equals(p.getBusinessId())).findFirst();
                if (compare.isPresent()) {
                    input.remove(compare.get());
                } else {
                    toDoService.finishTodo(p.getType(), p.getBusinessId());
                }
            });
            if (input.size() > 0) {
                input.forEach(p -> {
                    toDoService.addTodo(p);
                });
            }
        }
    }

    @Override
    public void syncActivitiTaskAssigneeChanged(String processInstanceId, List<ToDoDto> input) {
        if (input != null) {
            QTodo qTodo = QTodo.todo;
            //旧的待办
            List<Todo> oldTodos = jpaQueryFactory.selectFrom(qTodo).where(qTodo.businessId.startsWith(processInstanceId)).fetch();
            oldTodos.forEach(p -> {
                Optional<ToDoDto> compare = input.stream().filter(o -> o.getBusinessId().equals(p.getBusinessId())).findFirst();
                if (compare.isPresent()) {
                    toDoService.finishTodo(p.getType(), p.getBusinessId());
                }
            });
            if (input.size() > 0) {
                input.forEach(p -> {
                    toDoService.addTodo(p);
                });
            }
        }
    }

    @Override
    public List<ToDoDto> findByPermission(ToDoTargetType targetType, String type, String typeName, List<String> permissionList) {
        QTodo qTodo = QTodo.todo;
        QTodoPermission qTodoPermission = QTodoPermission.todoPermission;
        JPAQuery<Integer> subQuery = jpaQueryFactory.selectOne().from(qTodoPermission)
                .where(qTodoPermission.permission.in(permissionList).and(qTodoPermission.todoId.eq(qTodo.id)));
        JPAQuery<Todo> query = jpaQueryFactory.selectFrom(qTodo).where(qTodo.targetType.eq(targetType).and(qTodo.finish.eq(false)));
        if (StringUtils.isNotBlank(typeName)) {
            query.where(qTodo.typeName.eq(typeName));
        }
        query.where(subQuery.exists());
        return query.fetch().stream().map(s -> {
            String[] targetData = s.getTodoPermissions().stream().map(TodoPermission::getPermission).toArray(String[]::new);
            ToDoDto t = new ToDoDto(s.getType(), s.getTypeName(), s.getTitle(), s.getState(), s.getTargetType(), targetData, s.getBusinessId(), s.getExtensions());
            return t;
        }).collect(Collectors.toList());
    }
}
