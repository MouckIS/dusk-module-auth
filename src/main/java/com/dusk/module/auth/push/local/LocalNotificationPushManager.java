package com.dusk.module.auth.push.local;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.core.pusher.*;
import com.dusk.module.auth.push.INotificationPushManager;
import com.dusk.module.auth.push.local.dto.NotificationContent;
import com.dusk.module.auth.push.local.dto.NotificationMsg;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author kefuming
 * @date 2022/11/24 7:47
 */
@Component
@Slf4j
@Conditional(LocalNotificationCondition.class)
@Primary
public class LocalNotificationPushManager implements INotificationPushManager {
    @Value("${app.notification.local.publish-url:}")
    private String publishUrl;

    @Value("${app.notification.local.timeout:2000}")
    private int timeout;

    private static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";


    @Override
    public void mobilePush(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation) {
        try{
            NotificationContent content = new NotificationContent();
            content.setTitle(pushMessage.getTitle());
            content.setBody(pushMessage.getBody());
            content.setDeviceType(pushMessage.getDeviceType());
            content.setExpireTime(pushMessage.getExpireTime().toString(TIME_FORMAT));
            content.setPushTime(pushMessage.getPushTime().toString(TIME_FORMAT));
            content.setStoreOffline(pushMessage.isStoreOffline());
            content.setTarget(pushMessage.getTarget());
            content.setTargetValue(pushMessage.getTargetValue());

            NotificationMsg msg = new NotificationMsg();
            msg.setMessageId(IdUtil.randomUUID());
            msg.setLevel(option.getNoticationLevel());
            msg.setNavigation(navigation);
            msg.setContent(content);

            String msgStr = JSONUtil.toJsonStr(msg);
            HttpResponse response = HttpUtil.createPost(publishUrl).timeout(timeout).body(msgStr).execute();
            if(!response.isOk()){
                log.error(StrUtil.format("消息发送失败,msg={}, 错误信息：{}",msgStr , response.body()));
            }
        }catch (Exception e){
            log.error("消息发送失败：", e);
        }

    }

    @Override
    @Async
    public void mobilePushAsync(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation) {
        mobilePush(pushMessage, option, pushType, navigation);
    }

    @Override
    public void smsPush(PushSMS input) {
        throw new UnsupportedOperationException("未支持改操作");
    }

    @Override
    public void smsPushAsync(PushSMS input) {
        throw new UnsupportedOperationException("未支持改操作");
    }
}
