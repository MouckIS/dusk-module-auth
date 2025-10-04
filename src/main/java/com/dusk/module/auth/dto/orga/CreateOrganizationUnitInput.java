package com.dusk.module.auth.dto.orga;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.module.auth.enums.OrgLabel;
import org.hibernate.validator.constraints.Length;


/**
 * @author kefuming
 * @date 2020-05-13 15:05
 */
@Data
public class CreateOrganizationUnitInput {
    @Schema(description = "父组织机构id")
    private Long parentId;

    @Schema(description = "名称")
    private String displayName;

    @Schema(description = "是否为厂站")
    private boolean station = false;

    @Schema(description = "序号")
    private int sortIndex;

    @Schema(description = "组织的类型")
    private EUnitType type = EUnitType.Inner;

    @Schema(description = "组织标签")
    private OrgLabel label;

    @Schema(description = "管理层人员id")
    private Long ManagerId;

    @Schema(description = "组织机构描述")
    @Length(max = 1000, message = "描述内容最多为1000字符")
    private String description;
}
