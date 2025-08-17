package com.dusk.module.auth.service;

import com.dusk.common.module.auth.dto.ToDoDto;
import com.dusk.module.auth.entity.Todo;
import com.dusk.module.auth.enums.ToDoMQTTTypeEnum;

/**
 * @author kefuming
 * @date 2021-02-04 17:03
 */
public interface ToDoPushService {

    /**
     * 推送mqtt消息
     *
     * @param todo
     * @param mqttTypeEnum
     */
    void pushMqttMsg(Todo todo, ToDoMQTTTypeEnum mqttTypeEnum);

    /**
     * 推送消息 包括手机顶部推送以及mqtt推送
     *
     * @param todo
     * @param dto
     * @param mqttTypeEnum
     */
    void pushMsg(Todo todo, ToDoDto dto, ToDoMQTTTypeEnum mqttTypeEnum);

    /**
     * 推送忽略待办mqtt消息
     *
     * @param todo
     */
    void pushIgnoreMsg(Todo todo, Long userId);
}
