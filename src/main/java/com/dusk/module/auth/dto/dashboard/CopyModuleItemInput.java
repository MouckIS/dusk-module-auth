package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class CopyModuleItemInput {

    /**
     * 拷贝源统计项Id
     */
    @Schema(description = "拷贝源统计项Id")
    @NotNull(message = "统计项Id不能为空")
    private Long sourceModuleItemId;

    /**
     * 目标模块Id
     */
    @Schema(description = "目标模块Id")
    @NotNull(message = "目标模块Id不能为空")
    private Long targetModuleId;
}
