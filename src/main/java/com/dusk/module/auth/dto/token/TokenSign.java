package com.dusk.module.auth.dto.token;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    @ApiModelProperty("唯一标识")
    private String identify;

    @NotEmpty(message = "角色不能为空")
    @ApiModelProperty("角色")
    private List<String> roles;

    @NotNull(message = "授权时长不能为空")
    @ApiModelProperty("授权时长（单位：天）")
    private Long time;

}
