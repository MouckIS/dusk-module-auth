package com.dusk.module.auth.dto.station;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @author kefuming
 * @date 2022/9/21 20:46
 */
@Getter
@Setter
public class AddUsersToStationInput {
    @ApiModelProperty("用户id列表")
    @NotEmpty(message = "用户id列表不能为空")
    private List<Long> userIds;

    @ApiModelProperty("厂站id")
    @NotNull(message = "厂站id不能为空")
    private Long stationId;
}
