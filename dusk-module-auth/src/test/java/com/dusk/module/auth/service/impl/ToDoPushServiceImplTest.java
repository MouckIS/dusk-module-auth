package com.dusk.module.auth.service.impl;

import com.dusk.common.mqs.utils.MqttUtils;
import com.dusk.module.auth.dto.ToDoDto;
import com.dusk.module.auth.enums.ToDoTargetType;
import com.dusk.module.auth.service.IUserRpcService;
import com.dusk.module.auth.entity.Todo;
import com.dusk.module.auth.entity.TodoPermission;
import com.dusk.module.auth.enums.ToDoMQTTTypeEnum;
import com.dusk.module.auth.manage.IUserManage;
import com.dusk.module.notification.service.INotificationPushRpcServicve;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ToDoPushServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ToDoPushServiceImplTest {

    @Mock
    private MqttUtils mqttUtils;
    @Mock
    private IUserManage userManage;
    @Mock
    private IUserRpcService userRpcService;
    @Mock
    private INotificationPushRpcServicve pushManager;

    private ToDoPushServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ToDoPushServiceImpl();
        ReflectionTestUtils.setField(service, "mqttUtils", mqttUtils);
        ReflectionTestUtils.setField(service, "userManage", userManage);
        ReflectionTestUtils.setField(service, "userRpcService", userRpcService);
        ReflectionTestUtils.setField(service, "pushManager", pushManager);
    }

    private static Todo todo(ToDoTargetType type, String... permissions) {
        Todo todo = new Todo();
        todo.setTitle("待办标题");
        todo.setTargetType(type);
        List<TodoPermission> list = new ArrayList<>();
        for (String p : permissions) {
            TodoPermission tp = new TodoPermission();
            tp.setPermission(p);
            list.add(tp);
        }
        todo.setTodoPermissions(list);
        return todo;
    }

    private static ToDoDto dto() {
        ToDoDto dto = new ToDoDto();
        dto.setTypeName("类型名");
        return dto;
    }

    @Test
    @DisplayName("pushMqttMsg：targetType=UserId 时按用户 Id 推送")
    void pushMqttMsgWithUserIdTarget() {
        service.pushMqttMsg(todo(ToDoTargetType.UserId, "11", "22"), ToDoMQTTTypeEnum.ADD);

        verify(mqttUtils, org.mockito.Mockito.times(2)).publishMsgAsync(anyString(), any(), anyInt());
        verify(userManage, never()).getUserIdsByRoleName(any());
    }

    @Test
    @DisplayName("pushMqttMsg：targetType=Role 时按角色查询用户")
    void pushMqttMsgWithRoleTarget() {
        when(userManage.getUserIdsByRoleName(any())).thenReturn(List.of(7L));

        service.pushMqttMsg(todo(ToDoTargetType.Role, "roleA"), ToDoMQTTTypeEnum.FINISH);

        verify(userManage).getUserIdsByRoleName(any());
        verify(mqttUtils).publishMsgAsync(anyString(), any(), anyInt());
    }

    @Test
    @DisplayName("pushMqttMsg：targetType=Permission 时按权限查询用户")
    void pushMqttMsgWithPermissionTarget() {
        when(userRpcService.getUserIdsByPermissionsOr(any())).thenReturn(List.of(9L));

        service.pushMqttMsg(todo(ToDoTargetType.Permission, "permA"), ToDoMQTTTypeEnum.IGNORE);

        verify(userRpcService).getUserIdsByPermissionsOr(any());
        verify(mqttUtils).publishMsgAsync(anyString(), any(), anyInt());
    }

    @Test
    @DisplayName("pushIgnoreMsg：单用户忽略消息推送")
    void pushIgnoreMsgPublishesToSingleUser() {
        service.pushIgnoreMsg(todo(ToDoTargetType.UserId, "1"), 5L);

        verify(mqttUtils).publishMsgAsync(anyString(), any(), anyInt());
    }

    @Test
    @DisplayName("pushMsg：未开启 APP 推送时不调用推送管理器")
    void pushMsgSkipsAppPushWhenDisabled() {
        ToDoDto dto = dto();
        dto.setAutoAppPush(false);

        service.pushMsg(todo(ToDoTargetType.UserId, "3"), dto, ToDoMQTTTypeEnum.ADD);

        verify(pushManager, never()).mobilePush(any(), any(), any(), any());
    }

    @Test
    @DisplayName("pushMsg：开启推送且标题/正文为空时回退到待办信息，推送管理器可用")
    void pushMsgFallsBackToTodoWhenAppFieldsBlank() {
        ToDoDto dto = dto();
        dto.setAutoAppPush(true);
        dto.setAppTitle("");
        dto.setAppBody("");

        service.pushMsg(todo(ToDoTargetType.UserId, "3"), dto, ToDoMQTTTypeEnum.ADD);

        verify(pushManager).mobilePush(any(), any(), any(), any());
    }

    @Test
    @DisplayName("pushMsg：开启推送且标题/正文已设置，推送管理器不可用时只记录日志")
    void pushMsgUsesProvidedAppFieldsWithoutPushManager() {
        ReflectionTestUtils.setField(service, "pushManager", null);

        ToDoDto dto = dto();
        dto.setAutoAppPush(true);
        dto.setAppTitle("APP标题");
        dto.setAppBody("APP正文");

        service.pushMsg(todo(ToDoTargetType.UserId, "3"), dto, ToDoMQTTTypeEnum.ADD);

        verify(pushManager, never()).mobilePush(any(), any(), any(), any());
    }
}
