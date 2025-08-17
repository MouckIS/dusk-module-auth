package com.dusk.module.auth.dto.datadisplay;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.dto.EntityDto;

/**
 * 〈〉
 *
 * @author kefuming
 * @create 2022/2/8
 * @since 1.0.0
 */
@Getter
@Setter
public class DataDisplayItemDto extends EntityDto {

    @ApiModelProperty("类型")
    private String displayType;
}
