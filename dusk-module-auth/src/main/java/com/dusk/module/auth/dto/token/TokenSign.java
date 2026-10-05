package com.dusk.module.auth.dto.token;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author : caiwenjun
 * @date : 2023/5/31 9:49
 */
@Getter
@Setter
public class TokenSign implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "唯一标识不能为空")
    @Schema(description = "唯一标识")
    private String identify;

    @NotEmpty(message = "角色不能为空")
    @Schema(description = "角色")
    private List<String> roles;

    @NotNull(message = "授权时长不能为空")
    @Schema(description = "授权时长（单位：天）")
    private Long time;

}
