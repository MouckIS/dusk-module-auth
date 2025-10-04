package com.dusk.module.auth.dto.orga;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;

/**
 * @author kefuming
 * @date 2020-05-13 15:29
 */
@Data
public class MoveOrganizationUnitInput extends EntityDto {
    @Schema(description = "新父组织机构id")
    private Long newParentId;
}
