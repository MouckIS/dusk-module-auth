package com.dusk.module.ddm.module.auth.dto.notification;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 将用户消息的状态标记为已读的输入Dto
 *
 * @author kefuming
 * @date 2021/1/5 9:53
 */
@Data
public class SetNotificationAsReadInput {

    /**
     * 消息的id集合列表
     */
    @ApiModelProperty("消息的id集合列表")
    @NotEmpty(message = "消息的Id不能为空")
    private List<Long> ids;

}
