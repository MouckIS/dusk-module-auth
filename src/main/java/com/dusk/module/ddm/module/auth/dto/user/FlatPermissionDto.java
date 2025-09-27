package com.dusk.module.ddm.module.auth.dto.user;

import lombok.Data;

/**
 * @author kefuming
 * @date 2020/5/18 9:59
 */
@Data
public class FlatPermissionDto {
    private String parentName;

    private String name;

    private String displayName;

    private String description;

    public boolean isGrantedByDefault;
}
