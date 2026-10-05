package com.dusk.module.auth.dto;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author pengjian
 * @date 2021-04-15 13:59
 */
@Getter
@Setter
public class RoleSimpleDto extends EntityDto {
    @Schema(description = "角色代码")
    private String roleCode;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "是否是默认角色")
    private boolean isDefault;
}
