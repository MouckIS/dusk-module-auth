package com.dusk.module.auth.push;

import com.dusk.common.mqs.enums.PushType;
import com.dusk.common.mqs.pusher.Navigation;
import com.dusk.common.mqs.pusher.NotificationOption;
import com.dusk.common.mqs.pusher.PushMessage;
import com.dusk.common.mqs.pusher.PushSMS;

/**
 * @author kefuming
 * @date 2022-01-24 15:44
 */
public interface INotificationPushManager {
    /**
     * 移动App消息推送
     *
     * @param pushMessage 消息对象
     * @param option      消息配置
     * @param pushType    推送类型，默认是NOTICE
     * @param navigation  推送接收后跳转的页面
     */
    void mobilePush(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation);

    void mobilePushAsync(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation);

    /**
     * 短信消息推送
     */
    void smsPush(PushSMS input);

    /**
     * 短信消息推送
     */
    void smsPushAsync(PushSMS input);
}
