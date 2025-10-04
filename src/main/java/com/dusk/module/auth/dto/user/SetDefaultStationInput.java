package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2020/5/26 8:50
 */
@Data
public class SetDefaultStationInput {
    @Schema(description = "厂站id")
    @NotNull(message = "厂站id不能为空")
    private Long stationId;
}
