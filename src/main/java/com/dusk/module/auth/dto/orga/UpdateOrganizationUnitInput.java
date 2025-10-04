package com.dusk.module.auth.dto.orga;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.enums.OrgLabel;

/**
 * @author kefuming
 * @date 2020-05-13 15:22
 */
@Data
public class UpdateOrganizationUnitInput extends EntityDto {
    @Schema(description = "父组织机构id")
    private Long parentId;

    @Schema(description = "名字")
    private String displayName;

    @Schema(description = "是否为厂站")
    private boolean station = false;

    @Schema(description = "序号")
    private int sortIndex;

    @Schema(description = "组织机构描述")
    private String description;

    @Schema(description = "组织标签")
    private OrgLabel label;

    // todo: 只有覆盖管理层员工id时， 这个字段才有用
    @Schema(description = "管理层人员id")
    private Long ManagerId;
}
