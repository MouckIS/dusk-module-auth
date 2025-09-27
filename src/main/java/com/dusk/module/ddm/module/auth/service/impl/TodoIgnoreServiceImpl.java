package com.dusk.module.ddm.module.auth.service.impl;

import com.querydsl.core.types.Predicate;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.entity.QTodoIgnore;
import com.dusk.module.auth.entity.TodoIgnore;
import com.dusk.module.auth.repository.ITodoIgnoreRepository;
import com.dusk.module.auth.service.ITodoIgnoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class TodoIgnoreServiceImpl extends BaseService<TodoIgnore,ITodoIgnoreRepository> implements ITodoIgnoreService  {

    @Override
    public boolean ignoreTodo(Long todoId) {
        boolean result = false;
        QTodoIgnore qTodoIgnore = QTodoIgnore.todoIgnore;
        Predicate predicate = qTodoIgnore.todoId.eq(todoId).and(qTodoIgnore.createId.eq(LoginUserIdContextHolder.getUserId()));
        if (!repository.exists(predicate)) {
            TodoIgnore todoIgnore = new TodoIgnore();
            todoIgnore.setTodoId(todoId);
            save(todoIgnore);
            result = true;
        }
        return result;
    }
}