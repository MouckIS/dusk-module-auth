package com.dusk.module.ddm.module.auth.dto.user;

import com.github.dozermapper.core.Mapping;
import lombok.Data;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2020/5/15 12:01
 */
@Data
public class UserListRoleDto implements Serializable {
    private String id;
    public String roleName;
}
