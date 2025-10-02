package com.dusk.module.auth.dto.station;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.NameValueDefaultByDto;

/**
 * @author kefuming
 * @date 2022/9/22 19:09
 */
@Getter
@Setter
public class StationsOfLoginUserDto extends NameValueDefaultByDto<Long> {
    @ApiModelProperty("是否为集控站-该厂站下有子站则认为该厂站为集控站")
    private boolean mainStation;
    @ApiModelProperty("父id")
    private Long parentId;
    @ApiModelProperty("序号")
    private int sortIndex;
}