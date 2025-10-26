package com.dusk.module.auth.dto.user;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/5/18 11:12
 */
@Getter
@Setter
public class UserListForLoginDto extends EntityDto {

    @Schema(description = "姓名")
    private String name;
    @Schema(description = "姓名拼音")
    private String surName;
    @Schema(description = "账户名")
    private String userName;
}
