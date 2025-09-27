package com.dusk.module.ddm.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.entity.TodoRead;
import com.dusk.module.auth.repository.ITodoReadRepository;
import com.dusk.module.auth.service.ITodoReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class TodoReadServiceImpl extends BaseService<TodoRead,ITodoReadRepository> implements ITodoReadService  {

    @Autowired
    private ITodoReadRepository todoReadRepository;

    @Override
    public void read(Long todoId) {
        Long userId = LoginUserIdContextHolder.getUserId();
        Optional<TodoRead> read = todoReadRepository.findByTodoIdAndUserId(todoId, userId);
        if (read.isEmpty()) {
            TodoRead todoRead = new TodoRead();
            todoRead.setTodoId(todoId);
            todoRead.setUserId(userId);
            save(todoRead);
        }
    }

    @Override
    public List<TodoRead> findByUserId(Long userId) {
        return todoReadRepository.findByUserId(userId);
    }
}