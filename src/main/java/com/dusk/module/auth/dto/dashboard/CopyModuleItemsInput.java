package com.dusk.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class CopyModuleItemsInput {

    /**
     * 拷贝源模块Id
     */
    @ApiModelProperty("拷贝源模块Id")
    @NotNull(message = "源模块Id不能为空")
    private Long sourceModuleId;

    /**
     * 目标模块Id
     */
    @ApiModelProperty("目标模块Id")
    @NotNull(message = "目标模块Id不能为空")
    private Long targetModuleId;
}
