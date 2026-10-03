package com.dusk.module.auth.push;

import com.dusk.common.mqs.enums.PushType;
import com.dusk.common.mqs.pusher.Navigation;
import com.dusk.common.mqs.pusher.NotificationOption;
import com.dusk.common.mqs.pusher.PushMessage;
import com.dusk.common.mqs.pusher.PushSMS;
import com.dusk.module.auth.dto.ToDoDto;
import org.springframework.scheduling.annotation.Async;

/**
 * @author kefuming
 * @date 2020-08-05 9:07
 */
public interface IPushManager {
    /**
     * 添加一个待办数据
     *
     * @param input
     */
    @Async
    void addTodo(ToDoDto input);

    /**
     * 完成一个待办
     *
     * @param type
     * @param businessId
     */
    @Async
    void finishTodo(String type, String businessId);

    /**
     * 添加一个待办，并且把历史存在的同类型同业务id的待办完成
     *
     * @param input
     */
    @Async
    void addTodoAndFinishOld(ToDoDto input);
}
