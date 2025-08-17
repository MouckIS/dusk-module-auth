package com.dusk.module.auth.dto.notification;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.VersionDto;
import com.dusk.common.module.auth.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * 用户消息列表的输出Dto
 *
 * @Author kefuming
 * @Date 2021/1/4 10:32
 */
@Data
public class NotificationListOutput extends VersionDto {

    /**
     * 消息标题
     */
    @ApiModelProperty("消息标题")
    private String title;

    /**
     * 消息是否已读
     */
    @ApiModelProperty("消息是否已读")
    private boolean read;

    /**
     * 消息创建时间
     */
    @ApiModelProperty("消息创建时间")
    private LocalDateTime createTime;

    /**
     * 消息类型
     */
    @ApiModelProperty("消息类型")
    private NotificationType type;

    /**
     * 消息类型值
     */
    @ApiModelProperty("消息类型值")
    private int typeValue;

    /**
     * 消息类型名称
     */
    @ApiModelProperty("消息类型名称")
    private String typeName;

    @ApiModelProperty("消息内容")
    private String content;


    private String pageNavigation;
}
