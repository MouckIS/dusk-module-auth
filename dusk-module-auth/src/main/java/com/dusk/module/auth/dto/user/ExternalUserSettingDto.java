package com.dusk.module.auth.dto.user;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * @author kefuming
 * @CreateTime 2022/12/27
 */
@Getter
@Setter
public class ExternalUserSettingDto extends EntityDto {
    @Schema(description = "账号名")
    @NotBlank(message = "账号名不能为空")
    private String userName;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "确认密码")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Schema(description = "所属厂站")
    private List<Long> stations;

    @Schema(description = "激活开始日期")
    private LocalDate activeStartDate;

    @Schema(description = "激活结束日期")
    private LocalDate activeEndDate;

    @Schema(description = "角色id列表 结果以入参为准, 字段为null则不处理")
    private List<Long> assignedRoleIds;

    @Schema(description = "用户是否激活 未激活无法使用 常见用于使用手机号验证激活或者邮箱验证激活")
    private boolean active;

    @Schema(description = "下次登陆必须强制修改密码")
    private boolean shouldChangePasswordOnNextLogin;
}
