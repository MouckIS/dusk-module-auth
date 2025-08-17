package com.dusk.module.auth.dto.fingerprint;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2021-05-12 10:05
 */
@Data
public class IdentifyInputDto {
    @ApiModelProperty(value = "指纹仪序列号", required = true)
    @NotNull(message = "用户id不允许为空")
    private Long userId;

    @ApiModelProperty("指纹记录id")
    private Long fingerprintId;

    @ApiModelProperty(value = "指纹仪序列号", required = true)
    @NotBlank(message = "指纹仪序列号不允许为空")
    private String deviceNo;
}
