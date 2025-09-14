package com.dusk.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.EntityDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class CreateOrUpdateClassify extends EntityDto {
    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    @ApiModelProperty("名称")
    private String name;

    /**
     * 布局
     */
    @NotBlank(message = "布局不能为空")
    @ApiModelProperty("布局")
    private String layoutId;

    /**
     * 主题Id
     */
    @NotNull(message = "主题Id不能为空")
    @ApiModelProperty("主题Id")
    private Long themeId;


    /**
     * 顺序
     */
    @NotNull(message = "次序不能为空")
    @ApiModelProperty("次序")
    private Integer seq;

    /**
     * 区域数目
     */
    @NotNull(message = "区域数目不能为空")
    @ApiModelProperty("区域数目")
    private Integer zoneNum;

    /**
     * 区域列表
     */
    @ApiModelProperty("区域列表")
    List<CreateOrUpdateZone> zones;
}
