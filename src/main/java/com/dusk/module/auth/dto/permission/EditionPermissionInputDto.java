package com.dusk.module.auth.dto.permission;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-12-11 10:50
 */
@Getter
@Setter
public class EditionPermissionInputDto {
    @NotNull(message = "版本id不能为空")
    private Long id;
    private List<String> permissions;
}
