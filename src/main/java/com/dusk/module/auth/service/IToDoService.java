package com.dusk.module.auth.service;

import com.dusk.common.framework.service.IBaseService;
import com.dusk.common.module.auth.dto.ToDoDto;
import com.dusk.module.auth.dto.todo.GetTodosInput;
import com.dusk.module.auth.dto.todo.TodoInfoDto;
import com.dusk.module.auth.entity.Todo;
import com.dusk.module.auth.repository.IToDoRepository;
import org.springframework.data.domain.Page;

/**
 * @author kefuming
 * @date 2020-08-04 14:33
 */
public interface IToDoService extends IBaseService<Todo, IToDoRepository> {
    /**
     * 添加待办
     *
     * @param input
     */
    void addTodo(ToDoDto input);

    /**
     * 完成待办
     *
     * @param type
     * @param businessId
     */
    void finishTodo(String type, String businessId);

    /**
     * 当前用户忽略待办
     * @param todoId
     */
    void ignoreTodo(Long todoId);

    Page<TodoInfoDto> getTodos(GetTodosInput input);

    void read(Long id);
}
