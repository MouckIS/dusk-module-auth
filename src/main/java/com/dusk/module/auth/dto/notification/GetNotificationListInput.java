package com.dusk.module.auth.dto.notification;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;
import com.dusk.common.module.auth.enums.NotificationType;

import javax.validation.constraints.NotNull;

/**
 * @Author kefuming
 * @Date 2020/12/24 15:50
 */
@Data
public class GetNotificationListInput extends PagedAndSortedInputDto {

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
