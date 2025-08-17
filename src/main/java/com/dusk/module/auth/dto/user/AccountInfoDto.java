package com.dusk.module.auth.dto.user;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Author kefuming
 * @CreateTime 2022-10-25
 */
@Getter
@Setter
public class AccountInfoDto {
    /**
     * 账号
     */
    private String userName;
    /**
     * 账号关联的角色
     */
    private List<UserRoleDto> roles;
    /**
     * 签字图片
     */
    private Long signaturePictureId;
    /**
     * 头像id
     */
    private Long profilePictureId;
}
