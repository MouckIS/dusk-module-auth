package com.dusk.module.ddm.module.auth.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.pusher.*;
import com.dusk.common.module.auth.service.IAuthPushRpcService;
import com.dusk.module.auth.push.INotificationPushManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author kefuming
 * @date 2021-04-26 10:02
 */
@Service
@Slf4j
public class PushRpcServiceImpl implements IAuthPushRpcService {
    @Autowired(required = false)
    INotificationPushManager notificationPushManager;

    @Override
    public void pushAppMsg(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation) {
        if (notificationPushManager != null) {
            notificationPushManager.mobilePush(pushMessage, option, pushType, navigation);
        } else {
            throw new BusinessException("尚未启用rabbitmq，无法推送消息");
        }

    }

    @Override
    public void pushAppMsgAsync(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation) {
        if (notificationPushManager != null) {
            notificationPushManager.mobilePushAsync(pushMessage, option, pushType, navigation);
        } else {
            log.error("尚未启用rabbitmq，无法推送消息");
        }

    }


    @Override
    public void smsPush(PushSMS input) {
        if (notificationPushManager != null) {
            notificationPushManager.smsPush(input);
        } else {
            throw new BusinessException("尚未启用rabbitmq，无法推送消息");
        }

    }

    @Override
    public void smsPushAsync(PushSMS input) {
        if (notificationPushManager != null) {
            notificationPushManager.smsPushAsync(input);
        } else {
            log.error("尚未启用rabbitmq，无法推送消息");
        }
    }


}
