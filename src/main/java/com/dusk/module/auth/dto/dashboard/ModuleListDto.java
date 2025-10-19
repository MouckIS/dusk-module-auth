package com.dusk.module.auth.dto.dashboard;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Getter
@Setter
public class ModuleListDto extends EntityDto {
    /**
     * 名称
     */
    @ApiModelProperty("名称")
    private String name;

    /**
     * code
     */
    @ApiModelProperty("code")
    private String code;

    /**
     * 是否中心模块
     */
    @ApiModelProperty("是否中心模块")
    private Boolean centerModule = false;
}
