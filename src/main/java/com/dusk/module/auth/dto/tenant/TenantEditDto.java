package com.dusk.module.auth.dto.tenant;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.VersionDto;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-04-30 15:26
 */
@Data
public class TenantEditDto extends VersionDto {
    /**
     * 租户代码
     */
    @NotBlank(message = "租户代码不能为空")
    @ApiModelProperty("租户代码")
    private String tenantName;

    /**
     * 租户名
     */
    @NotBlank(message = "租户名不能为空")
    @ApiModelProperty("租户名")
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
     * 是否在试用期
     */
    @ApiModelProperty("是否在试用期")
    private boolean inTrialPeriod;

    /**
     * 订阅到期时间（UTC）
     */
    @ApiModelProperty("订阅到期时间")
    private LocalDateTime subscriptionEndDateUtc;

    @ApiModelProperty("描述")
    private String description;

    /**
     * app下载地址
     */
    @ApiModelProperty("app下载地址")
    private String appDownloadUrl;
}
