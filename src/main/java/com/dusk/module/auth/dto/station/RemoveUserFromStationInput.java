package com.dusk.module.auth.dto.station;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2022/9/21 20:44
 */
@Getter
@Setter
public class RemoveUserFromStationInput {
    @Schema(description = "用户id")
    @NotNull(message = "用户id不能为空")
    private Long userId;
    
    @Schema(description = "厂站id")
    @NotNull(message = "厂站id不能为空")
    private Long stationId;
}
