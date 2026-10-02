package com.dusk.module.auth.dto.dashboard;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "名称")
    private String name;

    /**
     * code
     */
    @Schema(description = "code")
    private String code;

    /**
     * 是否中心模块
     */
    @Schema(description = "是否中心模块")
    private Boolean centerModule = false;
}
