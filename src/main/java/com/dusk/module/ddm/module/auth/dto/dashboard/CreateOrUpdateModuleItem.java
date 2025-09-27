package com.dusk.module.ddm.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.enums.dashboard.DashboardModuleType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class CreateOrUpdateModuleItem extends EntityDto {
    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    @ApiModelProperty("名称")
    private String name;

    /**
     * code
     */
    @NotBlank(message = "编号不能为空")
    @ApiModelProperty("编号")
    private String code;

    /**
     * 详情路径
     */
    @ApiModelProperty("详情路径")
    private String detailPath;
    /**
     * 数据来源
     */
    @ApiModelProperty("数据源")
    private String dataSource;

    /**
     * 类型
     */
    @NotNull(message = "类型不能为空")
    @ApiModelProperty("类型")
    private DashboardModuleType moduleType;

    /**
     * 模块Id
     */
    @NotNull(message = "模块Id不能为空")
    @ApiModelProperty("模块Id")
    private Long moduleId;

    /**
     * 图表类型
     */
    @ApiModelProperty("图表类型")
    private String chartType;
}
