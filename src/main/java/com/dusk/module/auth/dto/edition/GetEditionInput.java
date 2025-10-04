package com.dusk.module.auth.dto.edition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.dto.PagedAndSortedInputDto;

/**
 * @author kefuming
 * @date 2020-05-13 9:04
 */
@Data
public class GetEditionInput extends PagedAndSortedInputDto {
    @Schema(description = "根据版本名进行模糊查找")
    private String filter;
}
