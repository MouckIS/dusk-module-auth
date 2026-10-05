package com.dusk.module.auth.dto.station;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 *
 * @author caiwenjun
 * @date 2024/4/19 14:20
 */
@Getter
@Setter
public class AddStationDto implements Serializable {
    @Schema(description = "父id")
    private Long parentId;
    @Schema(description = "名称")
    private String displayName;
    @Schema(description = "序号")
    private int sortIndex;
    @Schema(description = "租户id")
    private Long tenantId;
    @Schema(description = "编码")
    private String code;
}
