package com.dusk.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author 王吉
 * @date 2021-03-29 18:15
 */

@Getter
@Setter
public class ChangePwdInput {
    @NotNull(message = "用户id不能为空")
    @Schema(description = "选中的用户id")
    private Long userId;
    @NotBlank(message = "密码不能为空")
    @Schema(description = "新密码")
    private String newPwd;
}
