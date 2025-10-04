package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/18 10:43
 */
@Data
public class UpdateUserPermissionsInput {
    @Schema(description = "用户id")
    public String userId;
    @NotBlank
    @Schema(description = "权限名称列表")
    public List<String> GrantedPermissionNames;
}
