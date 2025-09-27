package com.dusk.module.ddm.module.auth.dto.station;

import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("用户id")
    private Long id;
    @ApiModelProperty("姓名")
    private String name;
    @ApiModelProperty("账号")
    private String userName;
    @ApiModelProperty("邮箱地址")
    private String emailAddress;
    @ApiModelProperty("所属厂站id")
    private Long stationId;
    @ApiModelProperty("所属厂站名称")
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
