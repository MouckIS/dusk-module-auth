package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/18 10:43
 */
@Data
public class UpdateUserPermissionsInput {
    @ApiModelProperty("用户id")
    public String userId;
    @NotBlank
    @ApiModelProperty("权限名称列表")
    public List<String> GrantedPermissionNames;
}
