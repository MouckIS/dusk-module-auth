package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2021/7/27
 */
@Getter
@Setter
@Schema(description = "删除大类关联模块数据")
public class RemoveRelationModuleInput {
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
}
