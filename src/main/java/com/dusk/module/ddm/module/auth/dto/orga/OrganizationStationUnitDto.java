package com.dusk.module.ddm.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitDto;
import com.dusk.common.module.auth.enums.EUnitType;

/**
 * @author kefuming
 * @date 2022-04-20 13:47
 */
@Getter
@Setter
public class OrganizationStationUnitDto  extends OrganizationUnitDto {
    @ApiModelProperty("厂站是否可用")
    private boolean stationEnabled = false;
    @ApiModelProperty("组织类型")
    private EUnitType type;
    @ApiModelProperty("组织机构的描述")
    private String description;
    @ApiModelProperty("管理层id")
    private Long managerId;
}
