package com.dusk.module.auth.dto.user;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2020/5/15 12:01
 */
@Getter
@Setter
public class UserListRoleDto implements Serializable {
    private String id;
    public String roleName;
}
