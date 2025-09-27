package com.dusk.module.ddm.module.auth.dto.user;

import lombok.Data;

/**
 * @author kefuming
 * @date 2020/5/25 8:53
 */
@Data
public class ChangePasswordInput {

    private String oldPasswd;
    private String newPasswd;
}
