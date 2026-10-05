package com.dusk.module.auth.dto.dashboard;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2021/7/27
 */
@Getter
@Setter
@Schema(description = "统计模块查询条件数据")
public class GetThemeInput extends PagedAndSortedInputDto {
    @Schema(description = "主题名称")
    private String name;

    @Schema(description = "主题标题")
    private String title;

    @Schema(description = "主题样式")
    private String themeType;
}
