package com.dusk.module.auth.service;

import com.dusk.common.framework.service.IBaseService;
import com.dusk.module.auth.entity.TodoIgnore;
import com.dusk.module.auth.repository.ITodoIgnoreRepository;

public interface ITodoIgnoreService extends IBaseService<TodoIgnore,ITodoIgnoreRepository> {
    /**
     * 当前用户忽略待办
     * @param todoId
     */
    boolean ignoreTodo(Long todoId);
}