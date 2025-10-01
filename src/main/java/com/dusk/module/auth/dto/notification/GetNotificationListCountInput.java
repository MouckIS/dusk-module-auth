package com.dusk.module.auth.dto.notification;

import com.dusk.commom.rpc.auth.enums.NotificationType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

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
    @ApiModelProperty("是否已读")
    private Boolean read;

    /**
     * 消息类型
     */
    @ApiModelProperty("消息类型")
    private NotificationType type;

}
