package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.dto.PagedAndSortedInputDto;

/**
 * @author jianjianhong
 * @date 2021/7/27
 */
@Data
@Schema(description = "统计模块查询条件数据")
public class GetModuleInput extends PagedAndSortedInputDto {
    @Schema(description = "模块名称")
    private String name;

    @Schema(description = "模块编码")
    private String code;
}
