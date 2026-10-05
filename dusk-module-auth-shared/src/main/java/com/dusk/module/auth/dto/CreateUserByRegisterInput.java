package com.dusk.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class CreateUserByRegisterInput implements Serializable {
    @Schema(description = "姓名")
    private String name;

    @Schema(description = "账号")
    private String userName;

    @Schema(description = "电子邮件地址")
    private String emailAddress;

    @Schema(description = "电话号码")
    private String phoneNo;

    @Schema(description = "密码")
    private String password;
}
