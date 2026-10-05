package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/5/26 8:50
 */
@Getter
@Setter
public class SetDefaultStationInput {
    @Schema(description = "厂站id")
    @NotNull(message = "厂站id不能为空")
    private Long stationId;
}
