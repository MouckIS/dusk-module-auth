package com.dusk.module.auth.dto.orga;

import com.dusk.common.core.dto.AuditedEntityDto;
import com.dusk.common.core.enums.EUnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author pengjian
 * @date 2020-05-13 14:02
 */
@Getter
@Setter
public class OrganizationUnitDto extends AuditedEntityDto {
    @Schema(description = "父组织机构id")
    private Long parentId;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String displayName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "是否为厂站")
    private boolean station = false;

    @Schema(description = "序号")
    private int sortIndex;

    @Schema(description = "版本")
    private int version;

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "类型")
    private EUnitType type;

    //@Schema(description = "标签")
    //private OrgLabel label;

    @Schema(description = "专业编码")
    private String specialityCode;

    @Schema(description = "专业名称")
    private String specialityName;

    @Schema(description = "唯一编码")
    private String serialNo;

    @Schema(description = "路径 parent path/serialNo, 中间以/分隔")
    private String path;
}
