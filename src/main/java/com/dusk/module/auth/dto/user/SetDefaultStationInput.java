package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2020/5/26 8:50
 */
@Getter
@Setter
public class SetDefaultStationInput {
    @ApiModelProperty("厂站id")
    @NotNull(message = "厂站id不能为空")
    private Long stationId;
}
