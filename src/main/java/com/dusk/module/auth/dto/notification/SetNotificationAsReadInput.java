package com.dusk.module.auth.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 将用户消息的状态标记为已读的输入Dto
 *
 * @author kefuming
 * @date 2021/1/5 9:53
 */
@Getter
@Setter
public class SetNotificationAsReadInput {

    /**
     * 消息的id集合列表
     */
    @Schema(description = "消息的id集合列表")
    @NotEmpty(message = "消息的Id不能为空")
    private List<Long> ids;

}
