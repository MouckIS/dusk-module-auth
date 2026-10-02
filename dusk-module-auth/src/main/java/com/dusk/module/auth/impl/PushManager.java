package com.dusk.module.auth.impl;

import com.dusk.common.mqs.enums.PushType;
import com.dusk.common.mqs.pusher.Navigation;
import com.dusk.common.mqs.pusher.NotificationOption;
import com.dusk.common.mqs.pusher.PushMessage;
import com.dusk.common.mqs.pusher.PushSMS;
import com.dusk.module.auth.dto.ToDoDto;
import com.dusk.module.auth.push.IPushManager;
import com.dusk.module.auth.service.IAuthPushRpcService;
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
    @DubboReference
    IAuthPushRpcService authPushRpcService;

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

    @Override
    public void pushAppMsg(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation) {
        authPushRpcService.pushAppMsg(pushMessage, option, pushType, navigation);
    }

    @Override
    public void pushAppMsgAsync(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation) {
        authPushRpcService.pushAppMsgAsync(pushMessage, option, pushType, navigation);
    }

    @Override
    public void smsPush(PushSMS input) {
        authPushRpcService.smsPush(input);
    }

    @Override
    public void smsPushAsync(PushSMS input) {
        authPushRpcService.smsPushAsync(input);
    }
}
