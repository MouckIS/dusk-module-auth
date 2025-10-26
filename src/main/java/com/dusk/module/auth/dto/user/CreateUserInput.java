package com.dusk.module.auth.dto.user;

import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.rpc.auth.dto.CreateOrUpdateUserInput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @CreateTime 2022-10-26
 */
@Getter
@Setter
public class CreateUserInput extends CreateOrUpdateUserInput {

    @Schema(description = "账号类型")
    @NotNull
    private EUnitType userType;
}
