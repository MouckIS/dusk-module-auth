package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class ModuleDetailDto extends EntityDto {
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

    /**
     * 所有的统计项
     */
    @Schema(description = "所有的统计项")
    private List<ModuleItemListDto> moduleItems;
}
