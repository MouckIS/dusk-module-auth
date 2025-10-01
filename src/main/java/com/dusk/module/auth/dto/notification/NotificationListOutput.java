package com.dusk.module.auth.dto.notification;

import com.dusk.commom.rpc.auth.enums.NotificationType;
import com.dusk.common.core.dto.VersionDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户消息列表的输出Dto
 *
 * @author kefuming
 * @date 2021/1/4 10:32
 */
@Data
public class NotificationListOutput extends VersionDto {

    @ApiModelProperty("消息标题")
    private String title;

    @ApiModelProperty("消息是否已读")
    private boolean read;

    @ApiModelProperty("消息创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("消息类型")
    private NotificationType type;

    @ApiModelProperty("消息类型值")
    private int typeValue;

    @ApiModelProperty("消息类型名称")
    private String typeName;

    @ApiModelProperty("消息内容")
    private String content;


    private String pageNavigation;
}
