package com.dusk.module.auth.dto.dashboard;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Getter
@Setter
public class CreateOrUpdateClassify extends EntityDto {
    /**
     * 区域列表
     */
    @Schema(description = "区域列表")
    List<CreateOrUpdateZone> zones;
    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    @Schema(description = "名称")
    private String name;
    /**
     * 布局
     */
    @NotBlank(message = "布局不能为空")
    @Schema(description = "布局")
    private String layoutId;
    /**
     * 主题Id
     */
    @NotNull(message = "主题Id不能为空")
    @Schema(description = "主题Id")
    private Long themeId;
    /**
     * 顺序
     */
    @NotNull(message = "次序不能为空")
    @Schema(description = "次序")
    private Integer seq;
    /**
     * 区域数目
     */
    @NotNull(message = "区域数目不能为空")
    @Schema(description = "区域数目")
    private Integer zoneNum;
}
