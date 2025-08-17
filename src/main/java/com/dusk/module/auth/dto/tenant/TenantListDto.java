package com.dusk.module.auth.dto.tenant;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.EntityDto;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-06 15:56
 */
@Data
public class TenantListDto extends EntityDto {

    @ApiModelProperty("租户代码")
    private String tenantName;
    @ApiModelProperty("租户名称")
    private String name;
    @ApiModelProperty("版本名称")
    private String editionDisplayName;
    @ApiModelProperty("连接url")
    private String connUrl;
    @ApiModelProperty("数据库帐户名")
    private String connUserName;
    @ApiModelProperty("数据库密码")
    private String connPassword;
    @ApiModelProperty("租户是否激活")
    private boolean active;
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
    @ApiModelProperty("订阅到期时间")
    private LocalDateTime subscriptionEndDateUtc;
    @ApiModelProperty("租户关联的版本的唯一标识id")
    private String editionId;
    @ApiModelProperty("描述")
    private String description;

    /**
     * app下载地址
     */
    @ApiModelProperty("app下载地址")
    private String appDownloadUrl;

}
