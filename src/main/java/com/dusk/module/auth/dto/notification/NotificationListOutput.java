package com.dusk.module.auth.dto.notification;

import com.dusk.common.rpc.auth.enums.NotificationType;
import com.dusk.common.core.dto.VersionDto;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息是否已读")
    private boolean read;

    @Schema(description = "消息创建时间")
    private LocalDateTime createTime;

    @Schema(description = "消息类型")
    private NotificationType type;

    @Schema(description = "消息类型值")
    private int typeValue;

    @Schema(description = "消息类型名称")
    private String typeName;

    @Schema(description = "消息内容")
    private String content;


    private String pageNavigation;
}
