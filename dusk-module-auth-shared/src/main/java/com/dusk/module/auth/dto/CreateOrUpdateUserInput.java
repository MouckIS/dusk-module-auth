package com.dusk.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author duanxiaokang
 * @date 2020/5/18 10:49
 */
@Getter
@Setter
public class CreateOrUpdateUserInput implements Serializable {

    private UserEditDto user;

    @Schema(description = "角色id列表 结果以入参为准, 字段为null则不处理")
    private List<Long> assignedRoleIds;
    @Schema(description = "是否发送邮件激活")
    private boolean sendActivationEmail;
    @Schema(description = "是否生成随机密码")
    private boolean setRandomPassword;
    @Schema(description = "机构id列表")
    private List<Long> organizationUnits;
    @Schema(description = "角色分组id列表")
    private List<Long> roleGroupIds;
}
