package com.dusk.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Getter
@Setter
public class CopyModuleItemInput {

    /**
     * 拷贝源统计项Id
     */
    @ApiModelProperty("拷贝源统计项Id")
    @NotNull(message = "统计项Id不能为空")
    private Long sourceModuleItemId;

    /**
     * 目标模块Id
     */
    @ApiModelProperty("目标模块Id")
    @NotNull(message = "目标模块Id不能为空")
    private Long targetModuleId;
}
