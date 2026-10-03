package com.dusk.module.auth.impl;

import com.dusk.module.auth.dto.ToDoDto;
import com.dusk.module.auth.push.IPushManager;
import com.dusk.module.auth.service.ITodoRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2020-08-05 9:08
 */
@Component
public class PushManager implements IPushManager {
    @DubboReference
    ITodoRpcService todoRpcService;

    @Override
    public void addTodo(ToDoDto input) {
        todoRpcService.addTodo(input);
    }

    @Override
    public void finishTodo(String type, String businessId) {
        todoRpcService.finishTodo(type, businessId);
    }

    @Override
    public void addTodoAndFinishOld(ToDoDto input) {
        todoRpcService.addTodoAndFinishOld(input);
    }
}
