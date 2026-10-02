package com.dusk.module.auth.dto.fingerprint;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2021-05-11 17:16
 */
@Getter
@Setter
public class RegisterFingerprintInputDto {
    @Schema(description = "用户id", required = true)
    @NotNull(message = "用户id不允许为空")
    private Long userId;

    @Schema(description = "指纹仪序列号", required = true)
    @NotBlank(message = "指纹仪序列号不允许为空")
    private String deviceNo;
}
