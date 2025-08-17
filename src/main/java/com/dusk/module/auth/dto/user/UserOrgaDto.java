package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.module.auth.enums.EUnitType;
import com.dusk.module.auth.enums.OrgLabel;

/**
 * @author kefuming
 * @date 2020/11/17
 */
@Data
public class UserOrgaDto {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("父组织机构id")
    private String parentId;

    @ApiModelProperty("编码")
    private String code;

    @ApiModelProperty("名称")
    private String displayName;

    @ApiModelProperty("是否为厂站")
    private boolean station = false;

    @ApiModelProperty("序号")
    private int sortIndex;

    @ApiModelProperty("组织的类型")
    private EUnitType type;

    @ApiModelProperty("组织标签")
    private OrgLabel label;
}
