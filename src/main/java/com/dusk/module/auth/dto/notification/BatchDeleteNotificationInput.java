package com.dusk.module.auth.dto.notification;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量删除用户消息的输入Dto
 *
 * @Author kefuming
 * @Date 2021/8/5 19:29
 */
@Data
public class BatchDeleteNotificationInput {

    /**
     * 用户告警消息的Id列表
     */
    @NotNull(message = "用户告警消息的Id列表不能为空")
    private List<Long> ids;

}
