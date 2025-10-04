package com.dusk.module.auth.dto.station;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author kefuming
 * @date 2022/9/21 20:46
 */
@Getter
@Setter
public class AddUsersToStationInput {
    @Schema(description = "用户id列表")
    @NotEmpty(message = "用户id列表不能为空")
    private List<Long> userIds;

    @Schema(description = "厂站id")
    @NotNull(message = "厂站id不能为空")
    private Long stationId;
}
