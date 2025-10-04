package com.dusk.module.auth.dto.station;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2022/9/21 21:02
 */
@Getter
@Setter
@FieldNameConstants
@NoArgsConstructor
public class StationUserListDto implements Serializable {
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

    public StationUserListDto(Long id, String name, String userName, String emailAddress, Long stationId, String stationName) {
        this.id = id;
        this.name = name;
        this.userName = userName;
        this.emailAddress = emailAddress;
        this.stationId = stationId;
        this.stationName = stationName;
    }
}
