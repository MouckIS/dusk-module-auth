package com.dusk.module.auth.dto.permission;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-12-11 11:38
 */
@Getter
@Setter
public class TenantPermissionInputDto {
    @NotNull(message = "租户id不能为空")
    private Long id;
    private List<PermissionInputDto> permissions;
}
