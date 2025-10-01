package com.dusk.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.entity.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class CreateOrUpdateZone extends BaseEntity {
    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    @ApiModelProperty("名称")
    private String name;

    /**
     * 布局方向
     */
    @NotBlank(message = "布局方向不能为空")
    @ApiModelProperty("布局方向")
    private String orientation;

    /**
     * 位置
     */
    @NotNull(message = "位置不能为空")
    @ApiModelProperty("位置")
    private Integer zonePosition;

    /**
     * 统计项列表
     */
    @ApiModelProperty("统计项列表")
    List<CreateOrUpdateZoneItemRef> zoneItems;
}
