package com.dusk.module.ddm.module.auth.service;

import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.entity.TodoRead;
import com.dusk.module.auth.repository.ITodoReadRepository;

import java.util.List;

public interface ITodoReadService extends IBaseService<TodoRead,ITodoReadRepository> {

    void read(Long todoId);

    List<TodoRead> findByUserId(Long userId);
}