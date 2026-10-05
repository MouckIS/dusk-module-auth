package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.ToDoDto;
import com.dusk.module.auth.enums.ToDoTargetType;

import java.util.List;

/**
 * 待办rpc接口
 *
 * @author kefuming
 * @date 2020-08-04 14:28
 */
public interface ITodoRpcService {
    /**
     * 添加一个待办数据
     *
     * @param input
     */
    void addTodo(ToDoDto input);

    /**
     * 完成一个待办
     *
     * @param type
     * @param businessId
     */
    void finishTodo(String type, String businessId);

    /**
     * 添加一个待办，并且把历史存在的同类型同业务id的待办完成
     *
     * @param input
     */
    void addTodoAndFinishOld(ToDoDto input);


    /**
     * 同步工作流任务
     *
     * @param processInstanceId
     * @param input
     */
    void syncActivitiTask(String processInstanceId, List<ToDoDto> input);

    /**
     * 工作流变更任务代理人，重新推送待办
     *
     * @param processInstanceId
     * @param input
     */
    void syncActivitiTaskAssigneeChanged(String processInstanceId, List<ToDoDto> input);

    /**
     * 搜索代办
     */
    List<ToDoDto> findByPermission(ToDoTargetType targetType, String type, String typeName, List<String> permissionList);
}
