package com.dusk.module.ddm.module.auth.dto.user;

import lombok.Data;

import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/18 9:58
 */
@Data
public class GetUserPermissionsForEditOutput {
    public List<FlatPermissionDto> permissions;
    public List<String> grantedPermissionNames;
}
