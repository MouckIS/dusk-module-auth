package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Getter
@Setter
public class CopyModuleItemsInput {

    /**
     * 拷贝源模块Id
     */
    @Schema(description = "拷贝源模块Id")
    @NotNull(message = "源模块Id不能为空")
    private Long sourceModuleId;

    /**
     * 目标模块Id
     */
    @Schema(description = "目标模块Id")
    @NotNull(message = "目标模块Id不能为空")
    private Long targetModuleId;
}
