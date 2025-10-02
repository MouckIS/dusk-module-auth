package com.dusk.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("父组织机构id")
    private Long parentId;

    @ApiModelProperty("名称")
    private String displayName;

    @ApiModelProperty("是否为厂站")
    private boolean station = false;

    @ApiModelProperty("序号")
    private int sortIndex;

    @ApiModelProperty("组织的类型")
    private EUnitType type = EUnitType.Inner;

    @ApiModelProperty("组织标签")
    private OrgLabel label;

    @ApiModelProperty("管理层人员id")
    private Long ManagerId;

    @ApiModelProperty("组织机构描述")
    @Length(max = 1000, message = "描述内容最多为1000字符")
    private String description;
}
