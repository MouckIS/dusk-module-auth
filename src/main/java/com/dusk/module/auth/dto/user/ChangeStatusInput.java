package com.dusk.module.auth.dto.user;

import com.dusk.common.core.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author kefuming
 * @CreateTime 2023/2/21
 */
@Getter
@Setter
public class ChangeStatusInput {
    @Schema(description = "用户列表")
    @NotEmpty(message = "用户列表不能为空")
    private List<Long> userIds;

    @Schema(description = "用户状态")
    @NotNull(message = "用户状态不能为空")
    private UserStatus status;
}
