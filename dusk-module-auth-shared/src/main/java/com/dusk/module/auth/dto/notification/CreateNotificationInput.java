package com.dusk.module.auth.dto.notification;

import com.dusk.module.auth.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author kefuming
 * @date 2020/12/25 11:59
 */
@Getter
@Setter
public class CreateNotificationInput implements Serializable {

    /**
     * 消息标题
     */
    @Schema(description = "消息标题")
    @NotBlank(message = "消息标题不能为空")
    private String title;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 消息类型
     */
    @Schema(description = "消息类型")
    private NotificationType type;

    /**
     * 消息的子类型
     */
    @Schema(description = "消息子类型")
    private String subType;

    /**
     * 消息附带的参数
     */
    @Schema(description = "消息附带的参数")
    @SuppressWarnings("java:S1948") // 抑制SonarQube检查
    private Object pageNavigation;

    /**
     * 接收消息的用户Id列表
     */
    @NotEmpty(message = "用户Id列表不能为空")
    @Schema(description = "接收消息的用户Id列表")
    private List<Long> userIds;
}
