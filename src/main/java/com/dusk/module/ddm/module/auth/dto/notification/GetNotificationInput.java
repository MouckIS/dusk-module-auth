package com.dusk.module.ddm.module.auth.dto.notification;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.VersionDto;

import javax.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2021/1/5 10:26
 */
@Data
public class GetNotificationInput extends EntityDto {

    /**
     * 消息Id
     */
    @ApiModelProperty("消息Id")
    @NotNull(message = "消息Id不能为空")
    private Long id;

}
