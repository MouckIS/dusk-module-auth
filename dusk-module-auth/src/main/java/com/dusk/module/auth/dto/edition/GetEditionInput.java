package com.dusk.module.auth.dto.edition;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-13 9:04
 */
@Getter
@Setter
public class GetEditionInput extends PagedAndSortedInputDto {
    @Schema(description = "根据版本名进行模糊查找")
    private String filter;
}
