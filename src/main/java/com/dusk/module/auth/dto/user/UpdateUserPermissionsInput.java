package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("用户id")
    public String userId;
    @NotBlank
    @ApiModelProperty("权限名称列表")
    public List<String> GrantedPermissionNames;
}
