package com.dusk.module.auth.repository;

import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.module.auth.entity.TodoRead;

import java.util.List;
import java.util.Optional;

public interface ITodoReadRepository extends IBaseRepository<TodoRead> {

    Optional<TodoRead> findByTodoIdAndUserId(Long todoId,Long userId);

    List<TodoRead> findByUserId(Long userId);
}