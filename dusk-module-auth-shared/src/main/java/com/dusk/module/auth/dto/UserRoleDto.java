package com.dusk.module.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author 王吉
 * @date 2020-07-24 14:46
 */
@Getter
@Setter
public class UserRoleDto implements Serializable {
    private Long id;
    private String roleName;
    private String roleCode;
}
