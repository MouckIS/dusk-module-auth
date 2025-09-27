package com.dusk.module.ddm.module.auth.dto.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;

import javax.validation.constraints.NotBlank;

/**
 * @author jianjianhong
 * @date 2021-07-26 10:10
 */
@Data
public class CreateOrUpdateModule extends EntityDto {
    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    @ApiModelProperty("模块名称")
    private String name;

    /**
     * code
     */
    @ApiModelProperty("编号")
    private String code;

    /**
     * 是否中心模块
     */
    @ApiModelProperty("是否中心模块")
    private Boolean centerModule = false;
}
