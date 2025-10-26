package com.dusk.module.auth.dto.station;

import com.dusk.common.core.dto.NameValueDefaultByDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2022/9/22 19:09
 */
@Getter
@Setter
public class StationsOfLoginUserDto extends NameValueDefaultByDto<Long> {
    @Schema(description = "是否为集控站-该厂站下有子站则认为该厂站为集控站")
    private boolean mainStation;
    @Schema(description = "父id")
    private Long parentId;
    @Schema(description = "序号")
    private int sortIndex;
}