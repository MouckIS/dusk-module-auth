package com.dusk.module.auth.dto.tenant;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-06 15:56
 */
@Getter
@Setter
public class TenantListDto extends EntityDto {

    @Schema(description = "租户代码")
    private String tenantName;
    @Schema(description = "租户名称")
    private String name;
    @Schema(description = "版本名称")
    private String editionDisplayName;
    @Schema(description = "连接url")
    private String connUrl;
    @Schema(description = "数据库帐户名")
    private String connUserName;
    @Schema(description = "数据库密码")
    private String connPassword;
    @Schema(description = "租户是否激活")
    private boolean active;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "订阅到期时间")
    private LocalDateTime subscriptionEndDateUtc;
    @Schema(description = "租户关联的版本的唯一标识id")
    private String editionId;
    @Schema(description = "描述")
    private String description;

    /**
     * app下载地址
     */
    @Schema(description = "app下载地址")
    private String appDownloadUrl;

}
