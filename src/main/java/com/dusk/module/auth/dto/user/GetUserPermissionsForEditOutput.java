package com.dusk.module.auth.dto.user;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/18 9:58
 */
@Getter
@Setter
public class GetUserPermissionsForEditOutput {
    public List<FlatPermissionDto> permissions;
    public List<String> grantedPermissionNames;
}
