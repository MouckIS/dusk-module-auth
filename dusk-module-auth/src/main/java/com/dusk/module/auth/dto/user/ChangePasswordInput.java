package com.dusk.module.auth.dto.user;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/5/25 8:53
 */
@Getter
@Setter
public class ChangePasswordInput {

    private String oldPasswd;
    private String newPasswd;
}
