package com.dusk.module.auth.common.rabbitmq;

/**
 *
 * @author caiwenjun
 * @date 2024/1/23 10:33
 */
public class RabbitConstant {

    /**
     * 人员取消门禁授权消息推送路由
     */
    public static final String CANCEL_USER_AUTH_FANOUT_EXCHANGE_V1 = "crux.auth.user.cancel.auth.fanout.v1";

}
