package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.module.auth.enums.UserStatus;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Author kefuming
 * @CreateTime 2023/2/21
 */
@Getter
@Setter
public class ChangeStatusInput {
    @ApiModelProperty("用户列表")
    @NotEmpty(message = "用户列表不能为空")
    private List<Long> userIds;

    @ApiModelProperty("用户状态")
    @NotNull(message = "用户状态不能为空")
    private UserStatus status;
}
