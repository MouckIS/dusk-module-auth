package com.dusk.module.auth.dto.edition;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.EntityDto;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-08 10:16
 */
@Data
public class EditionListDto extends EntityDto {
    @ApiModelProperty("名称")
    private String name;
    @ApiModelProperty("显示的名称")
    private String displayName;
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
