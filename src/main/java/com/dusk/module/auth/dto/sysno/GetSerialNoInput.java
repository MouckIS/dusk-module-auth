package com.dusk.module.auth.dto.sysno;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;

/**
 * @author kefuming
 * @date 2021-11-24 14:28
 */
@Getter
@Setter
public class GetSerialNoInput extends PagedAndSortedInputDto {
    @ApiModelProperty("单据类型")
    private String billType;
}
