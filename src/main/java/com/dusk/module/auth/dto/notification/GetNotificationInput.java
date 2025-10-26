package com.dusk.module.auth.dto.notification;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2021/1/5 10:26
 */
@Getter
@Setter
public class GetNotificationInput extends EntityDto {

    /**
     * 消息Id
     */
    @Schema(description = "消息Id")
    @NotNull(message = "消息Id不能为空")
    private Long id;

}
