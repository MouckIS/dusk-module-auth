package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dozermapper.core.Mapper;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.framework.pusher.NotificationOption;
import com.dusk.common.framework.pusher.PushMessage;
import com.dusk.common.framework.utils.MqttUtils;
import com.dusk.common.module.auth.dto.ToDoDto;
import com.dusk.common.module.auth.enums.ToDoTargetType;
import com.dusk.common.module.auth.service.IUserRpcService;
import com.dusk.module.auth.dto.todo.ToDoMQTTContent;
import com.dusk.module.auth.dto.todo.TodoInfoDto;
import com.dusk.module.auth.entity.Todo;
import com.dusk.module.auth.entity.TodoPermission;
import com.dusk.module.auth.enums.ToDoMQTTTypeEnum;
import com.dusk.module.auth.manage.IUserManage;
import com.dusk.module.auth.push.INotificationPushManager;
import com.dusk.module.auth.service.ToDoPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2021-02-04 17:04
 */
@Service
@Slf4j
public class ToDoPushServiceImpl implements ToDoPushService {

    //待办消息发布topic
    public final static String TODO_TOPIC = "T/{}/crux/todo";
    @Autowired
    MqttUtils mqttUtils;
    @Autowired
    Mapper dozerMapper;
    @Autowired
    IUserManage userManage;
    @Autowired
    IUserRpcService userRpcService;
    @Autowired(required = false)
    INotificationPushManager pushManager;

    @Override
    @Async
    public void pushMqttMsg(Todo todo, ToDoMQTTTypeEnum mqttTypeEnum) {
        List<Long> userIds = getTargetUserIds(todo);
        pushMqtt(todo, userIds, mqttTypeEnum);
    }


    @Override
    @Async
    public void pushMsg(Todo todo, ToDoDto dto, ToDoMQTTTypeEnum mqttTypeEnum) {
        List<Long> userIds = getTargetUserIds(todo);
        pushMqtt(todo, userIds, mqttTypeEnum);
        if (dto.isAutoAppPush()) {
            PushMessage pushMessage = new PushMessage();
            pushMessage.setTargetValue(ArrayUtil.join(userIds.toArray(), ","));
            if (StrUtil.isEmpty(dto.getAppBody())) {
                pushMessage.setBody(todo.getTitle());
            } else {
                pushMessage.setBody(dto.getAppBody());
            }
            if (StrUtil.isEmpty(dto.getAppTitle())) {
                pushMessage.setTitle(dto.getTypeName());
            } else {
                pushMessage.setTitle(dto.getAppTitle());
            }
            NotificationOption option = new NotificationOption();
            option.setNoticationLevel(dto.getNoticationLevel());
            if (pushManager != null) {
                pushManager.mobilePush(pushMessage, option, dto.getPushType(), dto.getNavigation());
            } else {
                log.error("尚未启用rabbitmq，无法推送消息");
            }

        }
    }

    @Override
    @Async
    public void pushIgnoreMsg(Todo todo, Long userId) {
        pushMqtt(todo, Collections.singletonList(userId), ToDoMQTTTypeEnum.IGNORE);
    }


    private List<Long> getTargetUserIds(Todo todo) {
        ToDoTargetType targetType = todo.getTargetType();
        List<Long> userIds = new ArrayList<>();
        List<TodoPermission> todoPermissions = todo.getTodoPermissions();
        switch (targetType) {
            case UserId:
                todoPermissions.forEach(p -> {
                    userIds.add(Long.valueOf(p.getPermission()));
                });
                break;
            case Role:
                userIds.addAll(userManage.getUserIdsByRoleName(todoPermissions.stream().map(TodoPermission::getPermission).collect(Collectors.toList())));
                break;
            case Permission:
                String[] permissions = todoPermissions.stream().map(TodoPermission::getPermission).toArray(String[]::new);
                userIds.addAll(userRpcService.getUserIdsByPermissionsOr(permissions));
                break;
            default:
                break;
        }
        return userIds;
    }

    private void pushMqtt(Todo todo, List<Long> userIds, ToDoMQTTTypeEnum mqttTypeEnum) {
        TodoInfoDto msg = dozerMapper.map(todo, TodoInfoDto.class);
        userIds.forEach(p -> {
            mqttUtils.publishMsgAsync(StrUtil.format(TODO_TOPIC, p), new ToDoMQTTContent(msg, mqttTypeEnum), 0);
        });
    }
}
