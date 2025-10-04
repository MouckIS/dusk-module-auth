package com.dusk.module.auth.dto.orga;

import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.enums.EUnitType;

/**
 * @author kefuming
 * @date 2022-04-20 13:47
 */
@Getter
@Setter
public class OrganizationStationUnitDto  extends OrganizationUnitDto {
    @Schema(description = "厂站是否可用")
    private boolean stationEnabled = false;
    @Schema(description = "组织类型")
    private EUnitType type;
    @Schema(description = "组织机构的描述")
    private String description;
    @Schema(description = "管理层id")
    private Long managerId;
}
