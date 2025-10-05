package com.dusk.module.auth.dto.notification;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量删除用户消息的输入Dto
 *
 * @author kefuming
 * @date 2021/8/5 19:29
 */
@Getter
@Setter
public class BatchDeleteNotificationInput {

    /**
     * 用户告警消息的Id列表
     */
    @NotNull(message = "用户告警消息的Id列表不能为空")
    private List<Long> ids;

}
