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

    /**
     * 主动推送手机顶部推送
     *
     * @param pushMessage
     * @param option
     * @param pushType
     * @param navigation
     */
    void pushAppMsg(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation);

    /**
     * 主动推送手机顶部推送 异步
     *
     * @param pushMessage
     * @param option
     * @param pushType
     * @param navigation
     */
    void pushAppMsgAsync(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation);

    /**
     * 主动推送一条短信 同步，该方法主要用于需要判断推送的状态
     *
     * @param input
     */
    void smsPush(PushSMS input);

    /**
     * 主动推送一条短信 异步，不是太关注发没发成功
     *
     * @param input
     */
    void smsPushAsync(PushSMS input);
}
