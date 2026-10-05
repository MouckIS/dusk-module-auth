package com.dusk.module.auth.dto;

import com.dusk.common.core.enums.EUnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author pengjian
 * @date 2024/12/19 9:40
 */
@Getter
@Setter
public class UserDepartmentDto implements Serializable {
    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "用户姓名")
    private String name;

    @Schema(description = "隶属组织机构（公司/部门）id")
    private Long affiliatedOrgId;

    @Schema(description = "隶属组织机构（公司/部门）")
    private String affiliatedOrgName;

    @Schema(description = "直属组织机构id")
    private Long orgId;
    @Schema(description = "直属组织机构")
    private String orgName;

    @Schema(description = "组织的类型")
    private EUnitType orgType;
}
