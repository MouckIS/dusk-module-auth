package com.dusk.module.auth.dto.user;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @CreateTime 2022-10-26
 */
@Getter
@Setter
public class GetUserInfoOutput {
    /**
     * 基本信息
     */
    private BaseUserInfoDto baseUserInfoDto;
    /**
     * 账号信息
     */
    private AccountInfoDto accountInfoDto;
}
