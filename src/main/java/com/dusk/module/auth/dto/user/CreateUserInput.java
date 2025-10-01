package com.dusk.module.auth.dto.user;

import com.dusk.common.rpc.auth.dto.CreateOrUpdateUserInput;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.enums.EUnitType;

import javax.validation.constraints.NotNull;

/**
 * @author kefuming
 * @CreateTime 2022-10-26
 */
@Getter
@Setter
public class CreateUserInput extends CreateOrUpdateUserInput {

    @ApiModelProperty("账号类型")
    @NotNull
    private EUnitType userType;
}
