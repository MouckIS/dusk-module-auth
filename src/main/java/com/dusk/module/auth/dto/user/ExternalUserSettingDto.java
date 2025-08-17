package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.dto.EntityDto;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

/**
 * @Author kefuming
 * @CreateTime 2022/12/27
 */
@Getter
@Setter
public class ExternalUserSettingDto extends EntityDto {
    @ApiModelProperty("账号名")
    @NotBlank(message = "账号名不能为空")
    private String userName;

    @ApiModelProperty("密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @ApiModelProperty("确认密码")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @ApiModelProperty("所属厂站")
    private List<Long> stations;

    @ApiModelProperty("激活开始日期")
    private LocalDate activeStartDate;

    @ApiModelProperty("激活结束日期")
    private LocalDate activeEndDate;

    @ApiModelProperty("角色id列表 结果以入参为准, 字段为null则不处理")
    private List<Long> assignedRoleIds;

    @ApiModelProperty("用户是否激活 未激活无法使用 常见用于使用手机号验证激活或者邮箱验证激活")
    private boolean active;

    @ApiModelProperty("下次登陆必须强制修改密码")
    private boolean shouldChangePasswordOnNextLogin;
}
