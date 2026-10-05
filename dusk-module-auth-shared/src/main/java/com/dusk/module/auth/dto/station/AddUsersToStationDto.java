package com.dusk.module.auth.dto.station;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @Author 李思
 * @Date 10:31 2023/12/28
 **/
@Getter
@Setter
public class AddUsersToStationDto implements Serializable {
    @Schema(description = "用户id列表")
    @NotEmpty(message = "用户id列表不能为空")
    private List<Long> userIds;

    @Schema(description = "厂站id")
    @NotNull(message = "厂站id不能为空")
    private Long stationId;
}
