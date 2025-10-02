package com.dusk.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.enums.OrgLabel;

/**
 * @author kefuming
 * @date 2020-05-13 15:22
 */
@Data
public class UpdateOrganizationUnitInput extends EntityDto {
    @ApiModelProperty("父组织机构id")
    private Long parentId;

    @ApiModelProperty("名字")
    private String displayName;

    @ApiModelProperty("是否为厂站")
    private boolean station = false;

    @ApiModelProperty("序号")
    private int sortIndex;

    @ApiModelProperty("组织机构描述")
    private String description;

    @ApiModelProperty("组织标签")
    private OrgLabel label;

    // todo: 只有覆盖管理层员工id时， 这个字段才有用
    @ApiModelProperty("管理层人员id")
    private Long ManagerId;
}
