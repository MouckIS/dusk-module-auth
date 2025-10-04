package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.module.auth.enums.OrgLabel;

/**
 * @author kefuming
 * @date 2020/11/17
 */
@Data
public class UserOrgaDto {
    @Schema(description = "id")
    private Long id;

    @Schema(description = "父组织机构id")
    private String parentId;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String displayName;

    @Schema(description = "是否为厂站")
    private boolean station = false;

    @Schema(description = "序号")
    private int sortIndex;

    @Schema(description = "组织的类型")
    private EUnitType type;

    @Schema(description = "组织标签")
    private OrgLabel label;
}
