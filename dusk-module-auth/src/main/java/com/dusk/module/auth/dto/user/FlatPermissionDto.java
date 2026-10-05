package com.dusk.module.auth.dto.user;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/5/18 9:59
 */
@Getter
@Setter
public class FlatPermissionDto {
    public boolean isGrantedByDefault;
    private String parentName;
    private String name;
    private String displayName;
    private String description;
}
