package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2021/7/27
 */
@Data
@Schema(description = "大类关联模块数据")
public class RelationModuleInput {
    /**
     * 分类Id
     */
    @Schema(description = "大类Id")
    private Long classifyId;

    /**
     * 模块id
     */
    @Schema(description = "模块id")
    private Long moduleId;

    /**
     * 布局位置
     */
    @Schema(description = "布局位置")
    private Integer layoutLocation;
}
