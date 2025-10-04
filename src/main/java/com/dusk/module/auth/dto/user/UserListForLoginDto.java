package com.dusk.module.auth.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;

/**
 * @author kefuming
 * @date 2020/5/18 11:12
 */
@Data
public class UserListForLoginDto extends EntityDto {

    @Schema(description = "姓名")
    private String name;
    @Schema(description = "姓名拼音")
    private String surName;
    @Schema(description = "账户名")
    private String userName;
}
