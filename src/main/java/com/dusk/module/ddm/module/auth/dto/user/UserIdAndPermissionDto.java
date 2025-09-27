package com.dusk.module.ddm.module.auth.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-09-23 15:06
 */
@Getter
@Setter
@AllArgsConstructor
public class UserIdAndPermissionDto {
    private Long id;
    private String name;
}
