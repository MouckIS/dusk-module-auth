package com.dusk.module.auth.dto.notification;

import com.dusk.common.rpc.auth.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 获取用户的消息数量的输入Dto
 *
 * @author kefuming
 * @date 2021/1/5 11:04
 */
@Data
public class GetNotificationListCountInput {

    /**
     * 是否已读
     */
    @Schema(description = "是否已读")
    private Boolean read;

    /**
     * 消息类型
     */
    @Schema(description = "消息类型")
    private NotificationType type;

}
