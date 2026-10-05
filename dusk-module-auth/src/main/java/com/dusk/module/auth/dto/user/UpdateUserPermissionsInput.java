package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/18 10:43
 */
@Getter
@Setter
public class UpdateUserPermissionsInput {
    @Schema(description = "用户id")
    public String userId;
    @NotBlank
    @Schema(description = "权限名称列表")
    public List<String> GrantedPermissionNames;
}
