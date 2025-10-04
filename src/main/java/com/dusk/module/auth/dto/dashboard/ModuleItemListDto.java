package com.dusk.module.auth.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.enums.dashboard.DashboardModuleType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class ModuleItemListDto extends EntityDto {
    /**
     * code
     */
    @Schema(description = "code")
    private String code;
    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;
    /**
     * 详情路径
     */
    @Schema(description = "详情路径")
    private String detailPath;
    /**
     * 数据来源
     */
    @Schema(description = "数据来源")
    private String dataSource;

    /**
     * 类型
     */
    @Schema(description = "类型")
    @Enumerated(EnumType.STRING)
    private DashboardModuleType moduleType;

    /**
     * 模块Id
     */
    @Schema(description = "模块Id")
    private Long moduleId;


    /**
     * 图表类型
     */
    @Schema(description = "图表类型")
    private String chartType;
}
