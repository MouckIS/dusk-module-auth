package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.module.auth.dto.CreateOrUpdateUserInput;
import com.dusk.common.module.auth.enums.EUnitType;

import jakarta.validation.constraints.NotNull;

/**
 * @Author kefuming
 * @CreateTime 2022-10-26
 */
@Getter
@Setter
public class CreateUserInput extends CreateOrUpdateUserInput {

    @ApiModelProperty("账号类型")
    @NotNull
    private EUnitType userType;
}
