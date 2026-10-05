package com.dusk.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author pengjian
 * @date 2023/2/9 19:15
 */
@Getter
@Setter
public class GenerateTokenForNonUserInput implements Serializable {
    @NotBlank(message = "唯一标识不能为空")
    private String identify;

    @NotEmpty(message = "角色不能为空")
    private List<String> roles;

    @Schema(description = "授权时长")
    private long time;

    @Schema(description = "时长单位")
    private TimeUnit unit;
}
