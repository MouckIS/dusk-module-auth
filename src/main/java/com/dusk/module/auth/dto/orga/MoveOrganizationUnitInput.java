package com.dusk.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.EntityDto;

/**
 * @author kefuming
 * @date 2020-05-13 15:29
 */
@Data
public class MoveOrganizationUnitInput extends EntityDto {
    @ApiModelProperty("新父组织机构id")
    private Long newParentId;
}
