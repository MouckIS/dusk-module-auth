package com.dusk.module.auth.service;

import com.dusk.common.mqs.enums.PushType;
import com.dusk.common.mqs.pusher.Navigation;
import com.dusk.common.mqs.pusher.NotificationOption;
import com.dusk.common.mqs.pusher.PushMessage;
import com.dusk.common.mqs.pusher.PushSMS;

/**
 * @author kefuming
 * @date 2021-04-26 10:01
 */
public interface IAuthPushRpcService {

    /**
     * 手机顶部推送消息 同步
     *
     * @param pushMessage
     * @param option
     * @param pushType
     * @param navigation
     */
    void pushAppMsg(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation);

    /**
     * 手机顶部推送消息  异步
     *
     * @param pushMessage
     * @param option
     * @param pushType
     * @param navigation
     */
    void pushAppMsgAsync(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation);

    /**
     * 短信消息推送
     */
    void smsPush(PushSMS input);

    /**
     * 短信消息推送 异步
     */
    void smsPushAsync(PushSMS input);
}
