package com.dusk.module.auth.dto.station;

import com.dusk.common.core.dto.AuditedEntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author pengjian
 * @date 2022/10/11 19:00
 */
@Getter
@Setter
public class StationDto extends AuditedEntityDto {
    @Schema(description = "父id")
    private Long parentId;
    @Schema(description = "名称")
    private String displayName;
    @Schema(description = "序号")
    private int sortIndex;
    @Schema(description = "版本")
    private int version;
    @Schema(description = "租户id")
    private Long tenantId;
    @Schema(description = "编码")
    private String code;
}
