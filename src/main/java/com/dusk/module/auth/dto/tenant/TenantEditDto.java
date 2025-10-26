package com.dusk.module.auth.dto.tenant;

import com.dusk.common.core.dto.VersionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-04-30 15:26
 */
@Getter
@Setter
public class TenantEditDto extends VersionDto {
    /**
     * 租户代码
     */
    @NotBlank(message = "租户代码不能为空")
    @Schema(description = "租户代码")
    private String tenantName;

    /**
     * 租户名
     */
    @NotBlank(message = "租户名不能为空")
    @Schema(description = "租户名")
    private String name;

    /**
     * 连接url
     */
    @Schema(description = "连接url")
    private String connUrl;

    /**
     * 数据库帐户名
     */
    @Schema(description = "数据库帐户名")
    private String connUserName;

    /**
     * 数据库密码
     */
    @Schema(description = "数据库密码")
    private String connPassword;

    /**
     * 租户是否激活 未激活无法使用
     */
    @Schema(description = "租户是否激活")
    private boolean active;

    /**
     * 版本
     */
    @Schema(description = "版本的唯一标识id")
    private Long editionId;

    /**
     * 是否在试用期
     */
    @Schema(description = "是否在试用期")
    private boolean inTrialPeriod;

    /**
     * 订阅到期时间（UTC）
     */
    @Schema(description = "订阅到期时间")
    private LocalDateTime subscriptionEndDateUtc;

    @Schema(description = "描述")
    private String description;

    /**
     * app下载地址
     */
    @Schema(description = "app下载地址")
    private String appDownloadUrl;
}
