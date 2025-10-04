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
public class GetThemeInput extends PagedAndSortedInputDto {
    @Schema(description = "主题名称")
    private String name;

    @Schema(description = "主题标题")
    private String title;

    @Schema(description = "主题样式")
    private String themeType;
}
