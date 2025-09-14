package com.dusk.module.auth.push;

import lombok.extern.slf4j.Slf4j;
import com.dusk.common.framework.pusher.*;
import com.dusk.common.framework.rabbitmq.RabbitMQEnableCondition;
import com.dusk.common.framework.utils.RabbitMQUtils;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * @author kefuming
 * @date 2022-01-24 15:44
 */
@Component
@Slf4j
@Conditional(RabbitMQEnableCondition.class)
public class NotificationPushManager implements INotificationPushManager {
    @Autowired(required = false)
    RabbitMQUtils rabbitMQUtils;
    @Autowired(required = false)
    AmqpAdmin amqpAdmin;


    /**
     * IOS系统App的Key
     */
    @Value("${aliyun-push.iosAppKey}")
    private String iosAppKey;

    /**
     * Android系统App的Key
     */
    @Value("${aliyun-push.androidAppKey}")
    private String androidAppKey;

    //#region 常量
    private final static String MOBILE_PUSH_QUEUE_NAME = "push.aliyun";

    private final static String SMS_PUSH_QUEUE_NAME = "sms.aliyun";


    @PostConstruct
    public void declareQueue() {
        if (amqpAdmin == null) {
            return;
        }
        amqpAdmin.declareQueue(new Queue(MOBILE_PUSH_QUEUE_NAME));
        amqpAdmin.declareQueue(new Queue(SMS_PUSH_QUEUE_NAME));
        log.info("推送服务正常启动");
    }

    /**
     * 移动App消息推送
     *
     * @param pushMessage 消息对象
     * @param option      消息配置
     * @param pushType    推送类型，默认是NOTICE
     * @param navigation  推送接收后跳转的页面
     */
    @Override
    public void mobilePush(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation) {

        if (pushType == null) {
            pushType = PushType.NOTICE;
        }
        //推送标题不大于15个字符
        if (pushMessage.getTitle().length() >= 15) {
            pushMessage.setTitle(pushMessage.getTitle().substring(0, 15));
        }
        Payload payload = new Payload(pushType, pushMessage, option, navigation);
        if (pushMessage.getDeviceType().equals(PushDeviceType.ALL) || pushMessage.getDeviceType().equals(PushDeviceType.ANDROID)) {
            payload.setAppKey(androidAppKey);
            rabbitMQUtils.publishMsg(MOBILE_PUSH_QUEUE_NAME, payload);
        }
        if (pushMessage.getDeviceType().equals(PushDeviceType.ALL) || pushMessage.getDeviceType().equals(PushDeviceType.iOS)) {
            payload.setAppKey(iosAppKey);
            rabbitMQUtils.publishMsg(MOBILE_PUSH_QUEUE_NAME, payload);
        }

    }

    @Async
    @Override
    public void mobilePushAsync(PushMessage pushMessage, NotificationOption option, PushType pushType, Navigation navigation) {
        try {
            mobilePush(pushMessage, option, pushType, navigation);
        } catch (Exception e) {
            log.error("移动App消息推送失败", e);
        }

    }

    /**
     * 短信消息推送
     */
    @Override
    public void smsPush(PushSMS input) {
        rabbitMQUtils.publishMsg(SMS_PUSH_QUEUE_NAME, input);
    }

    @Async
    @Override
    public void smsPushAsync(PushSMS input) {
        try {
            smsPush(input);
        } catch (Exception e) {
            log.error("推送短信验证码失败", e);
        }

    }
}
