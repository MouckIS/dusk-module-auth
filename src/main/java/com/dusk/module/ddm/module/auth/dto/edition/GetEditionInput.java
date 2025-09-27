package com.dusk.module.ddm.module.auth.dto.edition;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.PagedAndSortedInputDto;

/**
 * @author kefuming
 * @date 2020-05-13 9:04
 */
@Data
public class GetEditionInput extends PagedAndSortedInputDto {
    @ApiModelProperty("根据版本名进行模糊查找")
    private String filter;
}
