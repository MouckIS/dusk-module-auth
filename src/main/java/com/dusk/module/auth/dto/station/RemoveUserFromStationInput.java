package com.dusk.module.auth.dto.station;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2022/9/21 20:44
 */
@Getter
@Setter
public class RemoveUserFromStationInput {
    @ApiModelProperty("用户id")
    @NotNull(message = "用户id不能为空")
    private Long userId;
    
    @ApiModelProperty("厂站id")
    @NotNull(message = "厂站id不能为空")
    private Long stationId;
}
