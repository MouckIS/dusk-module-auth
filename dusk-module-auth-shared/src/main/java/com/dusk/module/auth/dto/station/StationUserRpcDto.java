package com.dusk.module.auth.dto.station;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class StationUserRpcDto implements Serializable {
    @Schema(description = "用户id")
    private Long id;
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "账号")
    private String userName;
    @Schema(description = "邮箱地址")
    private String emailAddress;
    @Schema(description = "所属厂站id")
    private Long stationId;
    @Schema(description = "所属厂站名称")
    private String stationName;
}
