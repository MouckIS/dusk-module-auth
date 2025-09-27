package com.dusk.module.ddm.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("code")
    private String code;
    /**
     * 名称
     */
    @ApiModelProperty("名称")
    private String name;
    /**
     * 详情路径
     */
    @ApiModelProperty("详情路径")
    private String detailPath;
    /**
     * 数据来源
     */
    @ApiModelProperty("数据来源")
    private String dataSource;

    /**
     * 类型
     */
    @ApiModelProperty("类型")
    @Enumerated(EnumType.STRING)
    private DashboardModuleType moduleType;

    /**
     * 模块Id
     */
    @ApiModelProperty("模块Id")
    private Long moduleId;


    /**
     * 图表类型
     */
    @ApiModelProperty("图表类型")
    private String chartType;
}
