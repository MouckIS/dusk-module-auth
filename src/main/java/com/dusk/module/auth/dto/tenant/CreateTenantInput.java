package com.dusk.module.auth.dto.tenant;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-04-30 10:01
 */
@Data
public class CreateTenantInput {
    /**
     * 是否公用数据库
     */
    @ApiModelProperty("是否公用数据库")
    private boolean useDefaultDb;

    /**
     * 租户代码
     */
    @ApiModelProperty("租户代码")
    @NotBlank(message = "租户代码不能为空")
    private String tenantName;

    /**
     * 租户名
     */
    @ApiModelProperty("租户名")
    @NotBlank(message = "租户名不能为空")
    private String name;

    /**
     * 连接url
     */
    @ApiModelProperty("连接url")
    private String connUrl;

    /**
     * 数据库帐户名
     */
    @ApiModelProperty("数据库帐户名")
    private String connUserName;

    /**
     * 数据库密码
     */
    @ApiModelProperty("数据库密码")
    private String connPassword;

    /**
     * 租户是否激活 未激活无法使用
     */
    @ApiModelProperty("租户是否激活")
    private boolean active;

    /**
     * 版本
     */
    @ApiModelProperty("版本的唯一标识id")
    private Long editionId;


    /**
     * 订阅到期时间（UTC）
     */
    @ApiModelProperty("订阅到期时间")
    private LocalDateTime subscriptionEndDateUtc;


    /**
     * 管理员邮箱地址
     */
    @ApiModelProperty("管理员邮箱地址")
    private String adminEmailAddress;

    /**
     * 管理员账户名
     */
    @ApiModelProperty("管理员账户名")
    @NotBlank(message = "管理员账户名不能为空")
    private String adminUserName;
    /**
     * 管理员密码
     */
    @ApiModelProperty("管理员密码")
    private String adminPassword;

    /**
     * 下次登录是否需要修改密码
     */
    @ApiModelProperty("下次登录是否需要修改密码")
    private boolean shouldChangePasswordOnNextLogin;

    /**
     * 是否发送激活账户邮件
     */
    @ApiModelProperty("是否发送激活账户邮件")
    private boolean sendActivationEmail;

    @ApiModelProperty("描述")
    private String description;

    /**
     * app下载地址
     */
    @ApiModelProperty("app下载地址")
    private String appDownloadUrl;
}
