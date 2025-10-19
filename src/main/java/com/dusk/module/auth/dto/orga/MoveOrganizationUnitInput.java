package com.dusk.module.auth.dto.orga;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-13 15:29
 */
@Getter
@Setter
public class MoveOrganizationUnitInput extends EntityDto {
    @ApiModelProperty("新父组织机构id")
    private Long newParentId;
}
