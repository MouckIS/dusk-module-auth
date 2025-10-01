package com.dusk.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-13 17:23
 */
@Data
public class ParentOrganizationOutput {
    @ApiModelProperty("组织机构id")
    private String id;

    @ApiModelProperty("组织机构名称")
    private String displayName;

    @ApiModelProperty("是否为厂站")
    private boolean station = false;

    private int sortId;
}
