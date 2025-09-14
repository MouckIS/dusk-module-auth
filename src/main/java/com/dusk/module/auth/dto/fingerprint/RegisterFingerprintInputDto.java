package com.dusk.module.auth.dto.fingerprint;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2021-05-11 17:16
 */
@Data
public class RegisterFingerprintInputDto {
    @ApiModelProperty(value = "用户id", required = true)
    @NotNull(message = "用户id不允许为空")
    private Long userId;

    @ApiModelProperty(value = "指纹仪序列号", required = true)
    @NotBlank(message = "指纹仪序列号不允许为空")
    private String deviceNo;
}
