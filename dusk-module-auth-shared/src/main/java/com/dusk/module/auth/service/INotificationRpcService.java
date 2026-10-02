package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.notification.CreateNotificationInput;

/**
 * @author 喻黎洋
 * @date 2020/12/24 15:50
 */
public interface INotificationRpcService {

    /**
     * 生成用户消息
     *
     * @param input
     */
    void createNotification(CreateNotificationInput input);
}
