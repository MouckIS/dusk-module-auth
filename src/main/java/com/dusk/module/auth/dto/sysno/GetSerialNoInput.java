package com.dusk.module.auth.dto.sysno;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.PagedAndSortedInputDto;

/**
 * @author kefuming
 * @date 2021-11-24 14:28
 */
@Getter
@Setter
public class GetSerialNoInput extends PagedAndSortedInputDto {
    @Schema(description = "单据类型")
    private String billType;
}
