package com.dusk.module.auth.dto.notification;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.module.auth.enums.NotificationType;

import jakarta.validation.constraints.NotNull;

/**
 * 获取用户的消息数量的输入Dto
 *
 * @Author kefuming
 * @Date 2021/1/5 11:04
 */
@Data
public class GetNotificationListCountInput {

    /**
     * 是否已读
     */
    @ApiModelProperty("是否已读")
    private Boolean read;

    /**
     * 消息类型
     */
    @ApiModelProperty("消息类型")
    private NotificationType type;

}
